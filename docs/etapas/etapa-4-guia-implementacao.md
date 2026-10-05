# Etapa 4 - Guia didático da implementação

## 1. Objetivo deste guia

Este documento explica como a Etapa 4 foi implementada no SysHospitalar e por que cada tecnologia foi usada. O foco não é repetir o código linha por linha, mas construir um modelo mental que permita acompanhar o fluxo completo, localizar as classes responsáveis e justificar as decisões arquiteturais.

Os documentos de planejamento continuam sendo a fonte das decisões originais:

- [Planejamento da notificação assíncrona](./etapa-4-planejamento-notificacao-email.md);
- [Planejamento da importação Batch](./etapa-4-planejamento-batch-pacientes.md).

Para executar e validar os cenários descritos aqui, consulte o [Guia de testes da Etapa 4](./etapa-4-guia-testes.md).

## 2. Três mecanismos para três necessidades diferentes

A Etapa 4 não substitui a API REST construída anteriormente. Ela acrescenta mensageria e processamento em lote onde esses mecanismos resolvem problemas específicos.

| Mecanismo | Analogia | Uso no SysHospitalar |
| --- | --- | --- |
| REST | Atendimento em um balcão: o solicitante espera a resposta para continuar. | Cadastrar pacientes e pedir ao `atendimentos-service` que persista um atendimento. |
| Mensageria | Serviço postal: o remetente entrega a carta e não precisa esperar o destinatário terminar o trabalho. | Enviar a confirmação do atendimento por e-mail depois que ele já foi criado. |
| Batch | Linha de produção: vários itens passam pelas mesmas estações de leitura, inspeção e montagem. | Importar vários pacientes de um CSV, normalizando, validando e persistindo os registros. |

A pergunta principal é: **quem precisa esperar pelo resultado?**

- Para criar um atendimento, a aplicação principal precisa saber se o `atendimentos-service` conseguiu persistir o registro. Essa parte continua síncrona por REST.
- O cliente não precisa esperar o Gmail aceitar o e-mail. Essa parte pode ser assíncrona por RabbitMQ.
- Um CSV contém vários registros que precisam do mesmo tratamento. Esse é o cenário adequado para Spring Batch.

## 3. Arquitetura resultante

```text
Cliente HTTP
    |
    | POST /atendimentos
    v
Aplicação principal ---- REST/OpenFeign ----> atendimentos-service
    |                                             |
    |                                             v
    |                                    PostgreSQL atendimentos
    |
    | depois da confirmação da persistência
    v
Produtor -> exchange -> fila -> consumidor -> Gmail SMTP
              RabbitMQ

Cliente HTTP
    |
    | POST /batch/pacientes/importacoes + CSV
    v
Aplicação principal -> Spring Batch -> PostgreSQL principal
```

O `atendimentos-service` permanece responsável somente pelos atendimentos. RabbitMQ, envio de e-mail e importação de pacientes ficam na aplicação principal. Assim, cada aplicação continua acessando apenas o banco sob sua responsabilidade.

## 4. Notificação assíncrona de atendimento

### 4.1. Onde termina a operação síncrona

O método de cadastro em `AtendimentoService` segue esta ordem:

1. valida o pedido recebido;
2. confirma que paciente e médico existem na aplicação principal;
3. chama o `atendimentos-service` por OpenFeign;
4. converte a resposta remota;
5. cria e publica `AtendimentoCriadoEvento`;
6. devolve o atendimento criado ao controller.

O evento só nasce depois da resposta bem-sucedida do serviço remoto. Essa ordem evita enviar uma confirmação sobre um atendimento que não foi persistido.

A fronteira de sucesso também é importante: depois que o serviço remoto confirma a criação, o atendimento existe. Se o RabbitMQ estiver indisponível, o produtor registra a falha, mas não transforma o sucesso do atendimento em erro HTTP. O cliente continua recebendo `201 Created`.

### 4.2. O evento como envelope

`AtendimentoCriadoEvento` funciona como o envelope entregue ao serviço postal. Ele leva somente o necessário para o consumidor produzir o e-mail:

```json
{
  "eventoId": "f6ccde56-cf95-4d0c-b183-bc4968b89f50",
  "tipoEvento": "ATENDIMENTO_CRIADO",
  "atendimentoId": 42,
  "pacienteNome": "Ana Silva",
  "pacienteEmail": "ana@example.com",
  "dataHoraAtendimento": "2026-10-10T14:30:00"
}
```

O evento não contém médico, diagnóstico, tipo de atendimento ou outros dados clínicos. Essa redução diminui a exposição de informações e o acoplamento entre produtor e consumidor.

O `eventoId` identifica aquela publicação nos logs. Ele não implementa idempotência: duas entregas do mesmo evento ainda podem produzir dois e-mails.

### 4.3. Componentes e responsabilidades

| Componente | Responsabilidade |
| --- | --- |
| `AtendimentoService` | Publicar somente depois que o atendimento foi confirmado pelo serviço remoto. |
| `AtendimentoCriadoEvento` | Definir o contrato JSON da mensagem. |
| `AtendimentoCriadoProducer` | Enviar o evento à exchange e absorver falhas de publicação sem desfazer o atendimento. |
| `RabbitMqConfig` | Declarar topologia, conversor JSON, retry e configuração do listener. |
| `AtendimentoCriadoConsumer` | Receber o evento e delegar o envio. |
| `EmailAtendimentoService` | Montar e enviar a mensagem de texto simples com `JavaMailSender`. |

O produtor e o consumidor são componentes diferentes mesmo estando na mesma aplicação. Essa separação permite desativar apenas o consumidor enquanto API e produtor continuam disponíveis.

### 4.4. Topologia do RabbitMQ

```text
AtendimentoCriadoProducer
        |
        | routing key: atendimento.criado
        v
atendimentos.exchange  (direct, durável)
        |
        v
notificacao-email.atendimento-criado  (fila durável)
        |
        +---- sucesso ----> reconhecimento da mensagem
        |
        `---- falha definitiva
                  |
                  | dead-letter routing key: atendimento.criado.falhou
                  v
          atendimentos.dlx  (direct, durável)
                  |
                  v
          notificacao-email.atendimento-criado.dlq
```

Uma `DirectExchange` encaminha a mensagem quando a routing key coincide exatamente com a binding key. A durabilidade informa ao broker que exchange e filas devem sobreviver a uma reinicialização, desde que os dados do RabbitMQ também estejam em volume persistente.

O `JacksonJsonMessageConverter` transforma o record Java em JSON na publicação e reconstrói o evento no consumidor.

### 4.5. Consumidor ativável

A propriedade `notificacoes.consumidor.ativo` controla o `autoStartup` do container do listener. Ela é preenchida pela variável:

```text
NOTIFICACOES_CONSUMIDOR_ATIVO=true|false
```

Com `false`, a aplicação, o endpoint REST e o produtor continuam ativos, mas ninguém retira mensagens da fila. É como manter a agência dos correios aberta para receber cartas enquanto o carteiro responsável pela entrega está temporariamente parado.

Ao reativar o consumidor, as mensagens acumuladas voltam a ser processadas.

### 4.6. Conteúdo do e-mail

`EmailAtendimentoService` usa `SimpleMailMessage`, texto simples e a data no formato `dd/MM/yyyy HH:mm`:

```text
Assunto: Confirmação de atendimento #42

Olá, Ana Silva.
Seu atendimento #42 foi registrado para 10/10/2026 14:30.
Esta é uma mensagem automática.
```

O envio usa Gmail SMTP na porta 587 com STARTTLS. A senha configurada deve ser uma senha de aplicativo, nunca a senha comum da conta.

### 4.7. Retry e DLQ

Uma falha SMTP pode ser temporária. O listener utiliza retry stateless com:

- primeira tentativa imediata;
- segunda tentativa após 2 segundos;
- terceira tentativa após 4 segundos.

No código, `maxRetries(2)` significa duas novas tentativas depois da tentativa original, totalizando três chamadas.

Se todas falharem, `RejectAndDontRequeueRecoverer` rejeita a mensagem sem recolocá-la na fila principal. Como a fila possui dead-letter exchange configurada, o RabbitMQ encaminha a mensagem para a DLQ.

A DLQ é comparável a uma área de quarentena: o problema não é escondido nem repetido infinitamente. A mensagem fica separada para inspeção. Esta etapa não implementa reprocessamento automático da DLQ.

### 4.8. Comportamento diante de falhas

| Falha | Resultado |
| --- | --- |
| Validação HTTP falha | O serviço remoto não é chamado e nenhum evento é publicado. |
| `atendimentos-service` falha | Nenhum evento é publicado; a API informa a falha conforme o tratamento existente. |
| RabbitMQ falha depois da persistência | O atendimento e o `201` são preservados; a falha é registrada e a notificação pode ser perdida. |
| Consumidor está parado | A mensagem permanece na fila enquanto o broker estiver disponível. |
| SMTP falha temporariamente | O consumidor realiza até três tentativas. |
| SMTP continua falhando | A mensagem é rejeitada e enviada à DLQ. |

### 4.9. “Pelo menos uma vez” e transactional outbox

RabbitMQ trabalha com entrega **pelo menos uma vez**. Imagine que o Gmail aceita o e-mail, mas a conexão cai antes de o consumidor confirmar a mensagem ao broker. O RabbitMQ pode entregá-la novamente, gerando uma duplicidade rara.

Também existe uma pequena janela entre a persistência do atendimento e a publicação. Se o processo cair nessa janela ou o broker estiver indisponível, o atendimento existe, mas o evento pode não existir.

Uma transactional outbox reduziria esse risco ao gravar atendimento e evento em uma mesma transação persistente. Ela não foi adicionada porque aumentaria o escopo acadêmico da etapa. Da mesma forma, não foi criada uma tabela de idempotência para bloquear e-mails duplicados.

## 5. Importação de pacientes com Spring Batch

### 5.1. Por que Batch

O cadastro REST individual trata uma pessoa por requisição. Um CSV exige que vários registros passem pelo mesmo processo e que a execução possua estado, contadores e tratamento de falhas.

A analogia é uma linha de produção:

- o **Job** é a ordem de produção completa;
- o **Step** é uma estação dessa linha;
- o **ItemReader** retira o próximo item da caixa de entrada;
- o **ItemProcessor** inspeciona, limpa e decide se o item pode continuar;
- o **ItemWriter** guarda os itens aprovados;
- o **chunk** é a caixa com uma quantidade limitada de itens tratada em uma transação.

### 5.2. Interface HTTP

O início da importação usa:

```http
POST /batch/pacientes/importacoes
Content-Type: multipart/form-data
arquivo=<pacientes.csv>
```

O endpoint retorna `202 Accepted`, pois aceitar o trabalho não significa que ele já terminou. O cabeçalho `Location` aponta para:

```http
GET /batch/pacientes/importacoes/{executionId}
```

Exemplo de resposta inicial:

```json
{
  "executionId": 15,
  "jobName": "importacaoPacientesJob",
  "status": "STARTING",
  "arquivo": "pacientes.csv",
  "iniciadoEm": "2026-10-04T14:30:00"
}
```

O cliente consulta o segundo endpoint até encontrar um estado terminal, normalmente `COMPLETED` ou `FAILED`.

### 5.3. Aceitação e armazenamento do arquivo

Antes de iniciar o Job, `PacienteImportacaoService` verifica:

- arquivo presente e não vazio;
- tamanho máximo de 2 MB;
- extensão `.csv`;
- cabeçalho exato;
- inexistência de outra importação ativa.

O cabeçalho exigido é:

```csv
nome,cpf,dataNascimento,sexo,telefone,email,ativo
```

O nome fornecido pelo cliente não é usado como nome físico. O conteúdo é salvo em `BATCH_INPUT_DIR` com um UUID. Isso evita colisões entre uploads e reduz o risco de manipulação de caminho.

### 5.4. Job assíncrono e parâmetros

O Job se chama `importacaoPacientesJob` e possui o Step `importarPacientesStep`. O disparo usa `JobOperator` e um executor com uma única thread.

Cada execução recebe:

- `execucaoUuid`: identificador único e parâmetro identificador do Job;
- `arquivoPath`: caminho interno do arquivo;
- `nomeOriginal`: nome usado apenas na resposta e nos metadados.

O UUID permite submeter novamente o mesmo arquivo como uma nova execução. Isso não duplica pacientes existentes porque CPF e e-mail são verificados durante o processamento.

O método que inicia a importação é sincronizado e o repositório Batch é consultado antes do início. Se outra execução estiver ativa, a API devolve `409 Conflict`.

### 5.5. Reader, processor e writer

```text
CSV UTF-8
   |
   v
FlatFileItemReader + PacienteCsvLineMapper
   |
   | PacienteCsvItem com número da linha
   v
PacienteImportacaoProcessor
   |
   | Paciente válido ou null para filtrar
   v
JpaItemWriter<Paciente>
   |
   v
PostgreSQL principal
```

O reader ignora o cabeçalho e exige sete colunas. O line mapper preserva o número da linha para que os logs indiquem a origem de um erro sem registrar nome, CPF ou e-mail.

O processor aplica as regras abaixo:

| Campo | Tratamento |
| --- | --- |
| `nome` | Remove espaços externos e compacta sequências de espaços internos. |
| `cpf` | Remove pontuação e exige exatamente 11 dígitos. |
| `dataNascimento` | Interpreta `yyyy-MM-dd` e rejeita data futura. |
| `sexo` | Converte para maiúscula e aceita somente `M` ou `F`. |
| `telefone` | Remove caracteres não numéricos e rejeita resultado vazio. |
| `email` | Remove espaços externos, converte para minúsculas e valida o formato. |
| `ativo` | Aceita `true` ou `false`; valor vazio significa `true`. |

O processor mantém conjuntos de CPF e e-mail conhecidos. Eles começam com os dados já existentes no banco e recebem os registros aprovados durante a execução. Assim, uma duplicidade pode ser detectada contra o banco ou dentro do próprio CSV.

O writer usa JPA e grava os pacientes no banco da aplicação principal.

### 5.6. Chunks de 10

O Step está configurado com chunk de 10. Em termos práticos, o Batch acumula até dez itens aprovados antes de executar a escrita e confirmar a transação daquele bloco.

A caixa de dez itens é uma analogia útil: se o transporte da caixa falha, a transação daquele bloco pode ser revertida sem desfazer necessariamente todos os blocos já confirmados. O tamanho 10 é suficiente para demonstrar o mecanismo sem otimização prematura para grandes volumes.

### 5.7. Filtrado não é o mesmo que ignorado

Os contadores expostos pela consulta possuem significados diferentes:

| Contador | Significado |
| --- | --- |
| `lidos` | Itens que o reader conseguiu entregar ao processamento. |
| `gravados` | Pacientes persistidos pelo writer. |
| `filtrados` | Linhas estruturalmente legíveis que o processor rejeitou por regra ou duplicidade. |
| `ignorados` | Erros estruturais de leitura tratados pela política de skip. |

Por exemplo, um e-mail inválido possui sete colunas e chega ao processor: ele é filtrado. Uma linha com apenas duas colunas não pode ser convertida pelo line mapper: ela é um erro estrutural ignorado pelo reader, dentro do limite configurado.

O Step permite no máximo 100 `FlatFileParseException`. O erro seguinte excede o limite e faz a execução terminar como `FAILED`.

### 5.8. Ciclo de vida do arquivo

`PacienteImportacaoJobListener` examina o estado final:

- `COMPLETED`: exclui o CSV temporário;
- qualquer outro estado, incluindo `FAILED`: conserva o arquivo para diagnóstico.

Essa política equilibra limpeza e investigabilidade. Arquivos concluídos não ocupam espaço indefinidamente; arquivos problemáticos continuam disponíveis para análise administrativa.

### 5.9. Metadados do Spring Batch

`spring.batch.job.enabled=false` impede que o Job seja executado automaticamente na inicialização da aplicação. Ele só começa quando o endpoint chama o operador.

`spring.batch.jdbc.initialize-schema=always` cria as tabelas de metadados usadas para registrar Job instances, executions, Step executions, parâmetros, estados e contadores. Essas tabelas ficam no PostgreSQL da aplicação principal.

## 6. Unicidade de CPF e e-mail

A importação tornou necessário garantir que registros duplicados não entrassem por outro caminho, como o endpoint REST. A proteção foi aplicada em camadas:

| Camada | Proteção |
| --- | --- |
| DTO | Normaliza o e-mail recebido pela API para minúsculas. |
| Entidade | Mantém o e-mail normalizado mesmo quando construído por outro fluxo. |
| Service | Consulta duplicidade antes de cadastro ou atualização e retorna `409`. |
| Repository | Fornece consultas por CPF/e-mail e variantes que excluem o próprio ID. |
| Banco | Constraints `uk_paciente_cpf` e `uk_paciente_email` protegem contra concorrência. |
| Handler global | Converte violação das constraints em `409`, sem revelar o outro paciente. |

As verificações do Service produzem mensagens melhores, mas não substituem as constraints. Duas requisições concorrentes podem passar pela consulta antes de qualquer uma gravar; nesse caso, o banco funciona como a última catraca.

Antes de usar um banco persistente antigo, duplicidades e diferenças apenas de caixa devem ser investigadas manualmente. A aplicação não altera nem exclui registros antigos automaticamente.

## 7. Configuração e infraestrutura

### 7.1. Dependências adicionadas

A aplicação principal utiliza os starters de AMQP, Mail e Batch, além do suporte de retry. Os testes usam Spring Batch Test e os módulos PostgreSQL e RabbitMQ do Testcontainers.

O `atendimentos-service` não recebeu essas dependências, preservando sua responsabilidade limitada à API de atendimentos.

### 7.2. Config Server e variáveis

As propriedades de desenvolvimento e produção externalizam:

- host, porta, usuário e senha do RabbitMQ;
- host, porta, usuário, senha e remetente SMTP;
- ativação do consumidor;
- exchange, filas e routing keys;
- inicialização do schema Batch;
- diretório temporário;
- limite do upload multipart.

Os valores padrão de topologia permanecem no código/configuração para tornar o ambiente reproduzível. Credenciais reais ficam somente em variáveis de ambiente.

### 7.3. Docker Compose

O serviço `rabbitmq` usa uma imagem com painel de gerenciamento, volume próprio, health check e participação nos profiles `dev` e `prod`.

Portas locais:

- `5672`: protocolo AMQP usado pela aplicação;
- `15672`: painel HTTP de gerenciamento.

Em containers, a aplicação usa o hostname `rabbitmq`. `localhost` dentro de um container apontaria para o próprio container, não para o broker.

## 8. Segurança e observabilidade

- Credenciais reais não são versionadas; `.env.example` contém apenas placeholders.
- Logs da mensageria usam IDs do evento e do atendimento.
- Logs do Batch usam número da linha e motivo sanitizado, sem copiar dados pessoais.
- O e-mail contém apenas nome, identificador e data/hora do atendimento.
- Respostas de status Batch apresentam contadores e resumo, não o conteúdo das linhas.
- A DLQ e os metadados Batch tornam falhas observáveis sem expor detalhes internos ao cliente HTTP.

## 9. O que ficou fora do escopo

- transactional outbox;
- persistência de idempotência para e-mail;
- reprocessamento automático da DLQ;
- template HTML de e-mail;
- novo microsserviço de notificações;
- armazenamento permanente dos CSVs concluídos;
- importação paralela de vários arquivos;
- correção automática de duplicidades antigas;
- notificações de atualização ou cancelamento de atendimento.

Esses limites são decisões conscientes. A Etapa 4 demonstra mensageria e Batch sem transformar a solução em uma arquitetura distribuída maior que o necessário.

## 10. Resultado arquitetural

A solução preserva o comportamento síncrono onde existe dependência imediata, desacopla o efeito posterior do e-mail e cria um fluxo próprio para conjuntos de registros. Em resumo:

```text
REST        = preciso saber agora se a operação principal funcionou
Mensageria  = o trabalho pode acontecer depois
Batch       = vários registros percorrem o mesmo processo controlado
```

Essa separação é o principal aprendizado da Etapa 4: tecnologias diferentes não são usadas por novidade, mas porque representam tempos, volumes e responsabilidades diferentes.
