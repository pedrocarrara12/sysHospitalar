# Etapa 4 - Guia completo de testes

## 1. Objetivo e tipos de evidência

Este roteiro valida a implementação descrita no [Guia didático da Etapa 4](./etapa-4-guia-implementacao.md). Ele separa duas formas de evidência:

- **teste automatizado**: reproduzível, isolado e executado pelo Maven, com PostgreSQL e RabbitMQ temporários;
- **demonstração manual**: comprova o comportamento visível no ambiente completo, no painel RabbitMQ, no Postman e, quando configurado pelo aluno, no Gmail real.

Um teste automatizado que simula `JavaMailSender` não comprova que uma conta Gmail real está configurada. Da mesma forma, receber um e-mail manualmente não substitui os testes de regressão. As duas evidências se complementam.

## 2. Pré-requisitos

- Java 21 ou compatível com o `pom.xml`;
- Docker Desktop ou Docker Engine em execução;
- Docker Compose disponível;
- Maven Wrapper do repositório;
- PowerShell 7 para os exemplos que usam `Invoke-RestMethod -Form`, ou Postman como alternativa;
- portas `5432`, `5433`, `5672`, `15672`, `8080`, `8081` e `8888` livres;
- conta Gmail com verificação em duas etapas e senha de aplicativo apenas para o teste manual de e-mail.

Confirme as ferramentas:

```powershell
java -version
docker version
docker compose version
.\mvnw.cmd -version
```

Todos os comandos deste guia partem da raiz do repositório.

## 3. Preparação segura do ambiente

Crie o arquivo local de variáveis:

```powershell
Copy-Item .env.example .env
```

Edite `.env` e troque os placeholders. O arquivo real é ignorado pelo Git.

Para Gmail:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-aplicativo
MAIL_FROM=seu-email@gmail.com
```

Não use a senha comum da conta, não cole credenciais em arquivos `.md`, não as envie ao Postman e não as registre em capturas de tela.

Antes de testar, confirme que `.env` não será versionado:

```powershell
git status --short -- .env
```

O resultado esperado é vazio.

## 4. Testes automatizados

### 4.1. Executar as três aplicações

Aplicação principal:

```powershell
.\mvnw.cmd test
```

Serviço de atendimentos:

```powershell
.\mvnw.cmd -f atendimentos-service\pom.xml test
```

Config Server:

```powershell
.\mvnw.cmd -f config-server\pom.xml test
```

O resultado obrigatório de cada comando é:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
```

Na implementação documentada, a suíte principal possui 15 testes. Essa contagem pode crescer; o critério principal é não haver falhas nem testes relevantes ignorados quando Docker está disponível.

### 4.2. O que o Testcontainers faz

Os testes de integração não usam os bancos persistentes do Compose. Testcontainers cria containers descartáveis para cada contexto:

```text
Teste inicia
   |
   +--> PostgreSQL temporário
   +--> RabbitMQ temporário, quando necessário
   |
Teste termina
   `--> containers são removidos
```

Isso fornece uma infraestrutura real sem depender de dados preexistentes. O Gmail é diferente: enviar mensagens reais em testes automatizados seria lento, instável e poderia disparar e-mails involuntários. Por isso, `JavaMailSender` é simulado.

### 4.3. Mapa das classes de teste

| Classe | Evidência principal |
| --- | --- |
| `AtendimentoServiceNotificacaoTest` | Publicação ocorre depois da confirmação remota; validação ou ausência de confirmação não publica. |
| `NotificacaoTest` | Remetente, destinatário, assunto e corpo; ausência de dados clínicos; falha do broker não escapa do produtor. |
| `NotificacaoRabbitIntegrationTest` | Publicação e consumo reais, consumidor parado, três chamadas SMTP simuladas e mensagem real na DLQ. |
| `PedroCarraraSyshospitalarApplicationTests` | PostgreSQL real, unicidade, Batch, endpoints, contadores, normalização, arquivo concluído excluído e arquivo com falha preservado. |
| `AtendimentosServiceApplicationTests` | Persistência e contexto do serviço independente sem regressão. |
| `ConfigServerApplicationTests` | Inicialização e configuração do Config Server sem regressão. |

### 4.4. Logs de falha esperados

Alguns testes provocam falhas deliberadamente. Durante uma execução bem-sucedida podem aparecer stack traces contendo:

- `SMTP indisponivel`;
- `Retries exhausted`;
- `Skip limit of '100' exceeded`;
- estado Batch `FAILED`.

Essas mensagens são parte dos cenários de DLQ e limite de erros. Verifique o resumo final do Maven antes de concluir que a suíte falhou.

### 4.5. Executar grupos específicos

Somente mensageria:

```powershell
.\mvnw.cmd test '-Dtest=AtendimentoServiceNotificacaoTest,NotificacaoTest,NotificacaoRabbitIntegrationTest'
```

Somente integração da aplicação e Batch:

```powershell
.\mvnw.cmd test -Dtest=PedroCarraraSyshospitalarApplicationTests
```

## 5. Validar o Docker Compose

Verifique a sintaxe sem iniciar serviços:

```powershell
docker compose --profile dev config --quiet
docker compose --profile prod config --quiet
```

Ambos devem terminar com código zero e sem erro de variável ou estrutura.

Suba apenas a infraestrutura de desenvolvimento:

```powershell
docker compose --profile dev up -d --wait
docker compose --profile dev ps
```

Devem aparecer saudáveis:

- `postgres-principal`;
- `postgres-atendimentos`;
- `rabbitmq`.

Valide o painel:

```powershell
(Invoke-WebRequest -UseBasicParsing http://localhost:15672).StatusCode
```

Resultado esperado: `200`.

O login usa `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD` do `.env`.

## 6. Iniciar o ambiente completo

A forma mais direta para a demonstração é o profile `prod` local:

```powershell
docker compose --profile prod up --build -d --wait
docker compose --profile prod ps
```

Os seis serviços esperados são:

1. PostgreSQL principal;
2. PostgreSQL de atendimentos;
3. RabbitMQ;
4. Config Server;
5. `atendimentos-service`;
6. aplicação principal.

Valide as interfaces:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/v3/api-docs | Select-Object StatusCode
Invoke-WebRequest -UseBasicParsing http://localhost:8081/v3/api-docs | Select-Object StatusCode
Invoke-WebRequest -UseBasicParsing http://localhost:8888/pedro-carrara-syshospitalar/prod | Select-Object StatusCode
Invoke-WebRequest -UseBasicParsing http://localhost:15672 | Select-Object StatusCode
```

Todos devem responder `200`.

Também é possível executar as aplicações pelo Maven, como descrito no README. Nesse caso, lembre-se de que o Maven não lê `.env` automaticamente: as variáveis precisam ser definidas no terminal ou na IDE.

## 7. Preparar dados para o atendimento

Em banco vazio, crie um médico:

```powershell
$medico = @{
  nome = "Medico Demonstracao"
  idade = 40
  cpf = "10987654321"
  email = "medico.demonstracao@example.com"
  ativo = $true
  crm = "CRM-DEMO-1"
  especialidade = "Clinica Geral"
} | ConvertTo-Json

$medicoCriado = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/medicos `
  -ContentType "application/json" `
  -Body $medico
```

Crie um paciente usando uma caixa de e-mail que você controla:

```powershell
$paciente = @{
  nome = "Paciente Demonstracao"
  cpf = "48392017654"
  dataNascimento = "1990-05-10"
  sexo = "F"
  telefone = "65999990000"
  email = "SEU-EMAIL-CONTROLADO@example.com"
  ativo = $true
} | ConvertTo-Json

$pacienteCriado = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/pacientes `
  -ContentType "application/json" `
  -Body $paciente
```

Se os valores já existirem, use CPF, CRM e e-mails diferentes. Guarde os IDs retornados.

## 8. Testar o envio real pelo Gmail

Monte o atendimento com data futura próxima:

```powershell
$atendimento = @{
  dataHoraAtendimento = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
  tipoAtendimento = "URGENCIA"
  statusAtendimento = "ANDAMENTO"
  pacienteId = $pacienteCriado.id
  medicoId = $medicoCriado.id
} | ConvertTo-Json

$atendimentoCriado = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/atendimentos `
  -ContentType "application/json" `
  -Body $atendimento
```

Confirme:

- resposta HTTP `201 Created` no Postman ou Swagger;
- log de publicação com `eventoId` e `atendimentoId`;
- fila principal sem mensagem acumulada depois do consumo;
- recebimento do e-mail na caixa do paciente;
- assunto e corpo sem médico, diagnóstico ou tipo do atendimento.

O recebimento real deve ser marcado como verificado somente depois desta execução manual.

## 9. Testar consumidor indisponível

Este cenário mantém broker, API e produtor funcionando e para somente o consumidor.

### 9.1. Desativar

No mesmo terminal usado para o Compose:

```powershell
$env:NOTIFICACOES_CONSUMIDOR_ATIVO="false"
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
```

Espere o health check da aplicação e crie outro atendimento.

### 9.2. Observar a fila

Abra `http://localhost:15672`, entre em **Queues and Streams** e selecione:

```text
notificacao-email.atendimento-criado
```

O contador **Ready** deve mostrar a mensagem aguardando. Nenhum e-mail deve chegar enquanto o consumidor estiver desativado.

### 9.3. Reativar

```powershell
$env:NOTIFICACOES_CONSUMIDOR_ATIVO="true"
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
```

Depois da inicialização, confirme que o contador diminui e o e-mail é enviado.

Ao terminar:

```powershell
Remove-Item Env:NOTIFICACOES_CONSUMIDOR_ATIVO -ErrorAction SilentlyContinue
```

## 10. Testar retry e DLQ

Guarde a configuração válida no `.env`. Sobrescreva apenas o processo de demonstração com uma senha inválida:

```powershell
$env:MAIL_PASSWORD="senha-invalida-para-demonstracao"
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
```

Crie outro atendimento e observe os logs:

```powershell
docker compose logs -f aplicacao-principal
```

Devem ocorrer três processamentos: tentativa inicial, nova tentativa após 2 segundos e última após mais 4 segundos.

No painel RabbitMQ, abra:

```text
notificacao-email.atendimento-criado.dlq
```

O contador **Ready** deve aumentar. Isso comprova que a falha não foi confirmada silenciosamente nem repetida para sempre.

Restaure a configuração:

```powershell
Remove-Item Env:MAIL_PASSWORD -ErrorAction SilentlyContinue
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
```

O Compose voltará a usar `MAIL_PASSWORD` do `.env`.

## 11. Teste opcional de indisponibilidade do broker

Pare somente o RabbitMQ:

```powershell
docker compose stop rabbitmq
```

Crie um atendimento. Se o `atendimentos-service` continuar disponível, o resultado esperado é o atendimento persistido e a resposta de sucesso, acompanhados de log informando falha na publicação.

Reative o broker:

```powershell
docker compose start rabbitmq
```

Essa mensagem específica não reaparecerá automaticamente. A possível perda é a limitação documentada pela ausência de transactional outbox.

## 12. Testar a importação Batch

### 12.1. Pelo Postman

Importe:

```text
postman/SysHospitalar-Etapa4.postman_collection.json
```

Na requisição **Iniciar importação de pacientes**, selecione no campo `arquivo`:

```text
src/main/resources/batch/pacientes-exemplo.csv
```

Envie a requisição. O esperado é:

- `202 Accepted`;
- cabeçalho `Location`;
- corpo com `executionId`;
- variável de coleção `executionId` preenchida pelo script.

Execute **Consultar importação** até obter `COMPLETED` ou `FAILED`.

### 12.2. Pelo PowerShell

```powershell
$inicio = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/batch/pacientes/importacoes `
  -Form @{ arquivo = Get-Item .\src\main\resources\batch\pacientes-exemplo.csv }

$inicio
```

Consulte:

```powershell
$status = Invoke-RestMethod `
  "http://localhost:8080/batch/pacientes/importacoes/$($inicio.executionId)"

$status
```

Polling simples:

```powershell
do {
  Start-Sleep -Milliseconds 500
  $status = Invoke-RestMethod `
    "http://localhost:8080/batch/pacientes/importacoes/$($inicio.executionId)"
  $status | Format-List
} while ($status.status -in @("STARTING", "STARTED"))
```

O arquivo de exemplo possui linhas válidas, inválidas e duplicadas. Portanto, é esperado que `gravados` seja menor que `lidos` e que `filtrados` seja maior que zero.

## 13. Cenários negativos do Batch

Use nomes temporários dentro de `target`, que não fazem parte da entrega.

### 13.1. Cabeçalho inválido

```powershell
New-Item -ItemType Directory -Force target\manual-tests | Out-Null
Set-Content -Encoding utf8 target\manual-tests\cabecalho-invalido.csv @"
nome,email
Ana,ana@example.com
"@
```

Envie o arquivo. Resultado esperado: `400 Bad Request` antes da criação do Job.

### 13.2. Arquivo vazio ou extensão incorreta

```powershell
New-Item -ItemType File -Force target\manual-tests\vazio.csv | Out-Null
Set-Content target\manual-tests\pacientes.txt "conteudo"
```

Resultados esperados:

- `vazio.csv`: `400`;
- `pacientes.txt`: `400`.

### 13.3. Arquivo acima de 2 MB

```powershell
$bytes = New-Object byte[] (2MB + 1)
[System.IO.File]::WriteAllBytes(
  (Join-Path $PWD "target\manual-tests\grande.csv"),
  $bytes
)
```

Resultado esperado: `400`, sem iniciar o Job.

### 13.4. Mais de 100 erros estruturais

```powershell
$linhas = @("nome,cpf,dataNascimento,sexo,telefone,email,ativo")
$linhas += 1..101 | ForEach-Object { "linha-malformada" }
$linhas | Set-Content -Encoding utf8 target\manual-tests\muitos-erros.csv
```

O upload deve retornar `202`, pois o cabeçalho é válido. A consulta posterior deve terminar como `FAILED`, e o arquivo temporário interno deve ser conservado para diagnóstico.

### 13.5. Importação concorrente

Inicie duas requisições quase simultaneamente. Enquanto a primeira estiver `STARTING` ou `STARTED`, a segunda deve receber `409 Conflict`.

Como o arquivo de exemplo termina rapidamente, use um CSV válido maior ou envie as duas chamadas por terminais separados. Se a primeira já estiver `COMPLETED`, o teste não criou concorrência e precisa ser repetido.

### 13.6. Execução inexistente

```powershell
Invoke-RestMethod http://localhost:8080/batch/pacientes/importacoes/9223372036854775807
```

Resultado esperado: `404 Not Found`.

## 14. Testar unicidade pela API REST

Depois de criar um paciente, tente cadastrar outro com o mesmo CPF e e-mail diferente. Resultado esperado: `409` com indicação de conflito no CPF.

Em seguida, use CPF diferente e o mesmo e-mail com letras maiúsculas. Resultado esperado: `409` com indicação de conflito no e-mail, comprovando a normalização para minúsculas.

Uma atualização que mantém o CPF e o e-mail do próprio paciente deve continuar válida, pois as consultas de atualização excluem o ID atual.

Não use o banco de produção para esse teste sem antes procurar duplicidades antigas. As constraints não corrigem dados preexistentes.

## 15. Inspeção segura

### RabbitMQ

No painel, observe apenas contadores, routing keys, estado dos consumidores e IDs técnicos. Evite abrir ou copiar payloads de ambientes com dados reais.

### Logs

Procure por:

- ID do evento;
- ID do atendimento;
- ID da execução Batch;
- número da linha e motivo sanitizado.

Não devem aparecer senha SMTP, senha do broker ou conteúdo completo do paciente.

### Arquivos Batch

O diretório é definido por `BATCH_INPUT_DIR`. Arquivo de execução concluída deve desaparecer. Arquivo de execução falha deve permanecer. Não publique esses arquivos como evidência se contiverem dados pessoais.

## 16. Encerramento e limpeza

Para parar os serviços preservando bancos e RabbitMQ:

```powershell
docker compose --profile prod down
```

O comando acima remove containers e redes, mas preserva volumes.

Não acrescente `-v` sem decidir conscientemente apagar os dados. Este comando é destrutivo para os volumes do projeto:

```text
docker compose --profile prod down -v
```

Remova apenas os arquivos temporários criados neste roteiro:

```powershell
Remove-Item -Recurse -Force target\manual-tests -ErrorAction SilentlyContinue
```

## 17. Solução de problemas

### Docker não responde

- confirme que Docker Desktop está iniciado;
- execute `docker version` e verifique se a seção Server aparece;
- feche containers antigos que usam as mesmas portas.

### Porta ocupada

Altere a porta publicada no `.env`, preservando a porta interna do container. Por exemplo, mudar `RABBITMQ_MANAGEMENT_PORT` altera apenas o endereço usado no navegador.

### RabbitMQ está saudável, mas a fila não consome

- confira `NOTIFICACOES_CONSUMIDOR_ATIVO`;
- confira os logs da aplicação principal;
- confirme que a aplicação usa host `rabbitmq` no profile de containers;
- verifique se há consumidor conectado à fila no painel.

### Gmail rejeita a autenticação

- use uma conta com verificação em duas etapas;
- gere uma senha de aplicativo;
- remova espaços copiados da senha;
- confira se `MAIL_USERNAME` e `MAIL_FROM` pertencem à conta usada;
- não use a senha comum da conta.

### Mensagem termina na DLQ

Isso significa que o consumidor recebeu a mensagem, mas o processamento falhou três vezes. Verifique primeiro a exceção SMTP. Corrigir a configuração não retira automaticamente mensagens que já estão na DLQ.

### CSV retorna `400`

- confirme extensão `.csv`;
- confirme UTF-8;
- confirme tamanho máximo de 2 MB;
- copie exatamente o cabeçalho obrigatório, sem trocar ordem ou nomes.

### Job permanece em `STARTED`

- confira os logs da thread `importacao-pacientes-*`;
- verifique conexão com PostgreSQL;
- não apague o arquivo temporário durante a execução;
- confirme que o processo da aplicação não foi encerrado abruptamente.

Um Job abandonado após queda do processo pode manter metadados que exigem análise administrativa. Esta etapa não expõe endpoint para reiniciar ou abandonar execuções.

## 18. Checklist para apresentação

### Evidência automatizada

- [ ] As três suítes terminam com `BUILD SUCCESS`.
- [ ] PostgreSQL e RabbitMQ são iniciados pelo Testcontainers.
- [ ] O teste de mensageria comprova consumo, retry e DLQ.
- [ ] O teste Batch comprova normalização, filtros, persistência e ciclo do arquivo.
- [ ] Os endpoints Batch retornam `202`, status consultável e `404` quando necessário.
- [ ] CPF e e-mail duplicados produzem conflito.

### Evidência manual

- [ ] Os seis serviços do profile `prod` estão saudáveis.
- [ ] Um atendimento retorna `201` e produz e-mail real.
- [ ] Com consumidor parado, a mensagem fica em `Ready`.
- [ ] Ao reativar o consumidor, a mensagem é processada.
- [ ] SMTP inválido produz três tentativas e DLQ.
- [ ] O CSV de exemplo produz execução e contadores coerentes.
- [ ] Um CSV com mais de 100 erros termina como `FAILED`.
- [ ] Nenhuma credencial aparece no repositório ou nas evidências.

### Documentação da evidência

Registre data, comando, resultado e captura relevante. Não registre senhas, payloads com dados pessoais ou arquivos reais de pacientes. A demonstração Gmail só deve ser marcada como concluída depois de o aluno confirmar o recebimento.
