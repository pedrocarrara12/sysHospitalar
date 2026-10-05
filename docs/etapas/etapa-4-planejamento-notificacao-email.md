# Etapa 4 - Planejamento da Notificação Assíncrona por E-mail

## Estado do documento

Este documento registra o desenho e a implementação da parte de mensageria da Etapa 4. Os itens automatizados foram marcados somente após existirem no código e serem verificados; o envio Gmail real continua pendente da demonstração manual pelo aluno.

O processamento em lote com Spring Batch não faz parte deste planejamento e terá um documento próprio.

## Operação escolhida

Quando um atendimento for criado com sucesso, a aplicação principal publicará uma mensagem para solicitar o envio de um e-mail de confirmação ao paciente.

O envio não precisa terminar durante a requisição original porque não altera o resultado da criação do atendimento. O cliente precisa receber imediatamente a confirmação de que o atendimento foi persistido, mas pode receber o e-mail alguns instantes depois.

O fluxo será:

```text
Cliente HTTP
    |
    v
POST /atendimentos na aplicação principal
    |
    v
atendimentos-service persiste o atendimento
    |
    v
aplicação principal recebe a confirmação
    |
    +--> retorna 201 Created ao cliente
    |
    `--> produtor publica AtendimentoCriadoEvento
             |
             v
          RabbitMQ
             |
             v
       consumidor assíncrono
             |
             v
          Gmail SMTP
             |
             v
      e-mail do paciente
```

A publicação deverá ocorrer somente depois de o `atendimentos-service` confirmar a persistência. Nenhuma mensagem deverá ser publicada quando a validação falhar ou quando o serviço remoto não criar o atendimento.

## Responsabilidades

### Aplicação principal

- validar a existência do paciente e do médico, como já ocorre atualmente;
- solicitar ao `atendimentos-service` a criação do atendimento;
- obter o nome e o e-mail do paciente no banco pertencente à aplicação principal;
- criar e publicar o evento somente após a resposta de sucesso do serviço remoto;
- consumir a mensagem e enviar o e-mail sem bloquear a requisição HTTP;
- registrar falhas sem expor credenciais ou o endereço completo do paciente nos logs.

### `atendimentos-service`

- continuar responsável exclusivamente pela persistência dos atendimentos;
- manter o contrato REST atual;
- não acessar dados de pacientes e não enviar e-mails.

### RabbitMQ

- desacoplar a criação do atendimento do envio do e-mail;
- manter a mensagem na fila enquanto o consumidor estiver desativado;
- encaminhar mensagens que esgotarem as tentativas de processamento para uma fila de mensagens mortas.

### Gmail SMTP

- realizar o envio real do e-mail;
- receber credenciais apenas por variáveis de ambiente;
- usar autenticação e STARTTLS na porta 587.

## Componentes planejados

Os componentes ficarão na aplicação principal, em um pacote próprio de notificações:

```text
notificacao
|-- config
|   `-- RabbitMqConfig
|-- evento
|   `-- AtendimentoCriadoEvento
|-- produtor
|   `-- AtendimentoCriadoProducer
|-- consumidor
|   `-- AtendimentoCriadoConsumer
`-- service
    `-- EmailAtendimentoService
```

- `RabbitMqConfig`: declara exchanges, filas, bindings, conversão JSON e política de retentativas.
- `AtendimentoCriadoProducer`: publica o evento e impede que uma falha do broker altere a resposta da criação do atendimento.
- `AtendimentoCriadoConsumer`: recebe o evento e delega o envio ao serviço de e-mail.
- `EmailAtendimentoService`: monta assunto e corpo em texto simples e usa `JavaMailSender`.

O `AtendimentoService` da aplicação principal será o ponto de integração com o produtor. Depois da resposta bem-sucedida do cliente Feign, ele montará o evento com os dados já conhecidos e solicitará sua publicação.

## Contrato do evento

O evento será representado pelo record `AtendimentoCriadoEvento`:

```java
public record AtendimentoCriadoEvento(
        UUID eventoId,
        String tipoEvento,
        Long atendimentoId,
        String pacienteNome,
        String pacienteEmail,
        LocalDateTime dataHoraAtendimento
) {
}
```

Regras do contrato:

- `eventoId` identifica a publicação e auxilia o rastreamento nos logs;
- `tipoEvento` terá o valor constante `ATENDIMENTO_CRIADO`;
- `atendimentoId` será o identificador retornado pelo `atendimentos-service`;
- nome e e-mail serão obtidos do paciente validado pela aplicação principal;
- `dataHoraAtendimento` será a data persistida e retornada pelo serviço;
- nenhum objeto JPA será serializado na mensagem;
- não serão enviados diagnóstico, CPF, telefone, médico, tipo ou status do atendimento.

Exemplo:

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

## Conteúdo do e-mail

O e-mail será enviado em texto simples para reduzir a complexidade e evitar exposição desnecessária de dados.

```text
Assunto: Confirmação de atendimento #42

Olá, Ana Silva.
Seu atendimento #42 foi registrado para 10/10/2026 14:30.
Esta é uma mensagem automática.
```

A formatação de data e hora usará o padrão `dd/MM/yyyy HH:mm`. O conteúdo não mencionará tipo de atendimento, médico, diagnóstico ou outras informações clínicas.

## Topologia do RabbitMQ

| Recurso | Nome | Característica |
|---|---|---|
| Exchange principal | `atendimentos.exchange` | `direct` e durável |
| Routing key principal | `atendimento.criado` | encaminha eventos de criação |
| Fila principal | `notificacao-email.atendimento-criado` | durável |
| Dead-letter exchange | `atendimentos.dlx` | `direct` e durável |
| Routing key de falha | `atendimento.criado.falhou` | encaminha falhas definitivas |
| Dead-letter queue | `notificacao-email.atendimento-criado.dlq` | durável |

A fila principal terá os argumentos:

```text
x-dead-letter-exchange=atendimentos.dlx
x-dead-letter-routing-key=atendimento.criado.falhou
```

As mensagens serão convertidas entre Java e JSON. Exchanges, filas e bindings deverão ser declarados pela aplicação para que a topologia seja criada de forma reproduzível.

## Retentativas e confirmação

O consumidor processará as mensagens com reconhecimento após o sucesso do listener:

- máximo de três tentativas no total;
- intervalo inicial de 2 segundos;
- multiplicador `2.0`, resultando em esperas de 2 e 4 segundos;
- falha definitiva rejeitada sem reenfileiramento na fila principal;
- mensagem rejeitada encaminhada para a DLQ pela configuração de dead letter.

Uma mensagem só será considerada processada depois que o `JavaMailSender` concluir sem exceção. As tentativas devem ser registradas sem incluir senha SMTP e sem imprimir o e-mail completo do paciente.

## Comportamento em falhas

### Consumidor desativado

A propriedade `NOTIFICACOES_CONSUMIDOR_ATIVO=false` impedirá a inicialização do listener, mas manterá a API e o produtor ativos. As mensagens publicadas permanecerão na fila. Ao reiniciar a aplicação com o consumidor ativo, elas serão processadas.

### RabbitMQ indisponível durante a publicação

A falha de publicação será capturada e registrada pelo produtor. O atendimento já criado será preservado e a API continuará retornando `201 Created`.

Esta etapa não implementará transactional outbox. Portanto, uma mensagem que não puder ser publicada poderá ser perdida. Essa limitação será informada no README e não deverá ser ocultada por uma resposta HTTP de erro que faça o cliente acreditar que o atendimento não foi criado.

### Gmail indisponível ou envio rejeitado

O consumidor realizará as três tentativas configuradas. Depois da última falha, a mensagem seguirá para a DLQ e poderá ser inspecionada pelo painel do RabbitMQ.

### Entrega duplicada

O RabbitMQ oferece entrega pelo menos uma vez, não exatamente uma vez. Uma falha entre o envio SMTP e a confirmação da mensagem pode resultar em um e-mail duplicado. O `eventoId` permitirá correlacionar os logs, mas não será criada uma tabela de idempotência nesta etapa.

## Configurações externas

As configurações da aplicação principal no Config Server deverão mapear:

```properties
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME}
spring.rabbitmq.password=${RABBITMQ_PASSWORD}

spring.rabbitmq.listener.simple.auto-startup=${NOTIFICACOES_CONSUMIDOR_ATIVO:true}
spring.rabbitmq.listener.simple.default-requeue-rejected=false

spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

notificacoes.email.remetente=${MAIL_FROM}
notificacoes.rabbitmq.exchange=${NOTIFICACOES_EXCHANGE:atendimentos.exchange}
notificacoes.rabbitmq.routing-key=${NOTIFICACOES_ROUTING_KEY:atendimento.criado}
notificacoes.rabbitmq.queue=${NOTIFICACOES_QUEUE:notificacao-email.atendimento-criado}
notificacoes.rabbitmq.dlx=${NOTIFICACOES_DLX:atendimentos.dlx}
notificacoes.rabbitmq.dlq=${NOTIFICACOES_DLQ:notificacao-email.atendimento-criado.dlq}
```

O `.env.example` deverá documentar placeholders para:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_MANAGEMENT_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
NOTIFICACOES_CONSUMIDOR_ATIVO
```

Para o Gmail, `MAIL_PASSWORD` será uma senha de aplicativo, nunca a senha comum da conta. Nenhuma credencial real deverá ser adicionada ao Git. Como o Maven não carrega `.env` automaticamente, a execução local fora do Compose exigirá que as variáveis sejam definidas no terminal ou na IDE.

## Docker Compose

Adicionar um serviço RabbitMQ baseado em uma imagem com o plugin de gerenciamento habilitado. O serviço participará dos profiles `dev` e `prod`, terá volume próprio e health check.

Portas locais planejadas:

- AMQP: `127.0.0.1:${RABBITMQ_PORT}:5672`;
- painel: `127.0.0.1:${RABBITMQ_MANAGEMENT_PORT}:15672`.

No ambiente `prod`, a aplicação principal usará `RABBITMQ_HOST=rabbitmq`, sem `localhost`, e dependerá do health check do broker. Usuário, senha e credenciais SMTP serão repassados por variáveis de ambiente.

## Dependências implementadas

Adicionar somente à aplicação principal:

- Spring Boot Starter AMQP;
- Spring Boot Starter Mail;
- suporte de retry utilizado pelo listener, caso não seja incluído transitivamente;
- módulo RabbitMQ do Testcontainers para os testes de integração.

O `atendimentos-service` não receberá dependências de RabbitMQ ou e-mail.

## Sequência aplicada

1. Adicionar dependências de AMQP, Mail e testes à aplicação principal.
2. Externalizar configurações de RabbitMQ, Gmail e ativação do consumidor.
3. Adicionar RabbitMQ, volume, rede, health check e variáveis ao Compose.
4. Criar o contrato `AtendimentoCriadoEvento`.
5. Declarar exchange, fila, DLX, DLQ, bindings, conversor JSON e política de retry.
6. Implementar o produtor com tratamento de `AmqpException` e logs correlacionados pelo `eventoId`.
7. Integrar a publicação ao fluxo de cadastro depois da resposta bem-sucedida do `atendimentos-service`.
8. Implementar o serviço de e-mail em texto simples.
9. Implementar o consumidor e tornar sua inicialização configurável.
10. Criar testes unitários e de integração.
11. Executar a demonstração manual com consumidor ativo, consumidor inativo e falha SMTP.
12. Atualizar o README com arquitetura, configuração, execução, evidências e limitações reais.

## Estratégia de testes

### Testes automatizados

- cadastro bem-sucedido solicita exatamente uma publicação depois da resposta remota;
- evento publicado contém `eventoId`, tipo, atendimento, nome, e-mail e data corretos;
- validação ou falha do `atendimentos-service` não publica mensagem;
- `AmqpException` no produtor não altera o retorno bem-sucedido do cadastro;
- consumidor delega uma mensagem válida ao serviço de e-mail;
- `JavaMailSender` recebe destinatário, remetente, assunto e corpo esperados;
- envio bem-sucedido permite a confirmação da mensagem;
- falha transitória gera três tentativas;
- falha após a terceira tentativa encaminha a mensagem à DLQ;
- consumidor desativado não retira mensagens da fila.

O `JavaMailSender` será simulado nos testes automatizados. Nenhum teste enviará e-mail real pelo Gmail.

### Demonstração manual

1. Configurar uma conta Gmail com verificação em duas etapas e senha de aplicativo.
2. Iniciar bancos, Config Server, `atendimentos-service`, RabbitMQ e aplicação principal.
3. Criar um atendimento para um paciente cujo e-mail seja controlado pelo responsável pela demonstração.
4. Confirmar o `201 Created`, a publicação, o consumo e o recebimento do e-mail.
5. Reiniciar a aplicação principal com `NOTIFICACOES_CONSUMIDOR_ATIVO=false`.
6. Criar outro atendimento e verificar no painel que a mensagem permanece na fila principal.
7. Reiniciar com o consumidor ativo e verificar que a mensagem é consumida e o e-mail é enviado.
8. Configurar temporariamente uma credencial SMTP inválida, publicar uma mensagem e observar as três tentativas e a entrada na DLQ.
9. Restaurar a configuração válida depois da demonstração.

## Critérios de aceite

- [x] O atendimento é criado antes da publicação do evento.
- [x] A resposta HTTP continua sendo `201 Created` sem aguardar o envio do e-mail.
- [x] O evento contém somente os dados definidos neste documento.
- [x] Exchange, fila, binding, DLX e DLQ são duráveis e reproduzíveis.
- [x] O consumidor pode ser desativado sem desativar a API ou o produtor.
- [x] A mensagem permanece no broker enquanto o consumidor está indisponível em teste de integração.
- [ ] O e-mail real é enviado pelo Gmail com o conteúdo discreto definido.
- [x] Falhas SMTP realizam três tentativas e terminam na DLQ em teste com RabbitMQ real e SMTP simulado.
- [x] Falha de publicação é absorvida pelo produtor e não desfaz nem mascara o atendimento já criado.
- [x] Credenciais reais não aparecem no código ou no repositório.
- [x] Testes automatizados não acessam o Gmail real.
- [x] O README explica quando usar REST, mensageria e Batch no SysHospitalar.

## Limitações assumidas

- Não haverá transactional outbox; falhas de publicação podem causar perda da notificação.
- Não haverá persistência para idempotência; uma entrega duplicada pode gerar e-mail duplicado.
- A DLQ será inspecionada manualmente; não haverá reprocessamento automático nesta etapa.
- O consumidor ficará na aplicação principal; não será criado um novo microsserviço.
- O e-mail será texto simples e não terá template HTML.
- Este fluxo cobre somente a criação de atendimento, não atualizações ou cancelamentos.
