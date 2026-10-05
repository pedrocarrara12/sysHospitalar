# SysHospitalar

API REST em Java e Spring Boot para gerenciamento de pacientes, medicos, enfermeiros e atendimentos hospitalares.

A aplicacao principal mantem os cadastros locais e expoe a API publica. A responsabilidade de atendimentos pertence ao `atendimentos-service`, consumido por HTTP com OpenFeign. Na Etapa 4, a criacao de um atendimento publica uma notificacao assincrona no RabbitMQ e a aplicacao principal tambem oferece importacao de pacientes por CSV com Spring Batch.

## Etapa 4 — Mensageria e processamento em lote

Nesta etapa, implementei duas funcionalidades seguindo os [requisitos de mensageria e processamento em lote](docs/etapas/etapa-4-assincrono-batch.md): a notificacao de atendimento por e-mail com RabbitMQ e a importacao de pacientes por CSV com Spring Batch.

Mantive a criacao do atendimento por REST, pois a aplicacao precisa confirmar sua persistencia antes de responder ao cliente. O envio de e-mail ocorre depois, por meio de um produtor, uma fila e um consumidor. Para importar varios pacientes, utilizei um Job com leitura, validacao, normalizacao e gravacao em chunks de 10.

Os testes automatizados utilizam PostgreSQL e RabbitMQ reais em containers e simulam o envio SMTP. Os resultados e os limites dessa cobertura estao em [Validacao e testes da Etapa 4](#validacao-e-testes-da-etapa-4). As secoes seguintes apresentam a arquitetura, as instrucoes de execucao e os roteiros de demonstracao.

## Tecnologias

- Java 21 e Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA e Bean Validation
- Spring Cloud OpenFeign e Spring Cloud Config
- PostgreSQL 17
- RabbitMQ 4, Spring AMQP e Gmail SMTP
- Spring Batch 6
- SpringDoc OpenAPI / Swagger
- Dockerfiles multi-stage e Docker Compose
- Testcontainers com PostgreSQL e RabbitMQ para testes de integracao
- Maven Wrapper

## Arquitetura

```text
Cliente HTTP
    |
    v
Aplicacao principal --------------------> PostgreSQL principal
    |
    +-- HTTP / OpenFeign --> atendimentos-service --> PostgreSQL atendimentos
    |
    | apos a confirmacao da persistencia pelo servico
    `-- produtor --> RabbitMQ --> consumidor na aplicacao principal --> SMTP

CSV --> Spring Batch na aplicacao principal --> PostgreSQL principal

Config Server
    |-------------------------------> Aplicacao principal
    `-------------------------------> atendimentos-service
```

Cada aplicacao acessa somente o banco pelo qual e responsavel:

- aplicacao principal: pacientes, medicos e enfermeiros;
- `atendimentos-service`: atendimentos, referenciando paciente e medico apenas por identificador.

O fluxo interno permanece organizado como:

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
```

Para atendimentos, a aplicacao principal atua como orquestradora:

```text
Cliente -> Controller -> Service -> Feign Client -> atendimentos-service
```

## Evolucao por etapas

### Etapa 1 - organizacao arquitetural

O monolito foi organizado por dominio, com controllers, services, repositories, DTOs, validacao e tratamento centralizado de excecoes. Os modulos identificados foram Pacientes, Equipe Clinica e Atendimentos. A dependencia principal e `Atendimentos -> Pacientes / Medicos`.

| Modulo | Responsabilidade |
| --- | --- |
| Pacientes | Cadastro, consulta, validacao de CPF/e-mail e, na Etapa 4, importacao de pacientes. |
| Equipe Clinica | Cadastros de medicos e enfermeiros, com especialidade, CRM, setor e COREN. |
| Atendimentos | Registro, consulta e alteracao de atendimentos, relacionando paciente e medico. |

Atendimentos foi o candidato a servico independente identificado nessa organizacao e extraido na Etapa 2: possui responsabilidade propria, enquanto depende dos identificadores dos cadastros principais.

### Etapa 2 - servico independente

Atendimentos foi separado como uma aplicacao Spring Boot independente. A aplicacao principal continua expondo o contrato publico e valida a existencia local do paciente e do medico antes de chamar o servico. Falhas de comunicacao sao convertidas em `503 Service Unavailable`, sem expor detalhes internos.

A persistencia de atendimentos saiu da aplicacao principal; ela enriquece a resposta publica com nomes locais e usa DTOs para o contrato HTTP. A separacao acrescentou configuracao de rede, dois processos e tratamento de indisponibilidade. Quando o servico cai, as operacoes que dependem dele falham de forma controlada; os cadastros locais continuam disponiveis. Para o porte academico, a responsabilidade poderia permanecer no monolito: sua independencia demonstra a fronteira entre dominios, sem exigir a extracao dos demais modulos.

### Etapa 3 - configuracao e execucao

A solucao passou a utilizar:

- profiles Spring `dev`, `prod`, `test` e `native`;
- configuracao centralizada por Config Server;
- dois bancos PostgreSQL com credenciais e volumes independentes;
- variaveis de ambiente para URLs, portas e credenciais;
- um Dockerfile por aplicacao executavel;
- um `compose.yml` com profiles `dev` e `prod`, redes, volumes e healthchecks.

O H2 utilizado nas etapas anteriores foi substituido por PostgreSQL nas duas APIs.

### Etapa 4 - assincrono e lote

Depois que o `atendimentos-service` confirma a persistencia, a aplicacao principal publica um `AtendimentoCriadoEvento`. O consumidor envia uma confirmacao simples ao e-mail do paciente, sem dados clinicos. Falhas SMTP recebem tres tentativas e, depois disso, seguem para uma DLQ.

A importacao Batch recebe CSV de pacientes, normaliza e valida cada linha e grava registros validos no PostgreSQL principal em chunks de 10. O inicio e a consulta da execucao sao expostos por endpoints separados.

Documentação aprofundada:

- [Requisitos e checklist da Etapa 4](docs/etapas/etapa-4-assincrono-batch.md);
- [Planejamento da notificacao por e-mail](docs/etapas/etapa-4-planejamento-notificacao-email.md);
- [Planejamento da importacao de pacientes](docs/etapas/etapa-4-planejamento-batch-pacientes.md);
- [Guia didático da implementação da Etapa 4](docs/etapas/etapa-4-guia-implementacao.md);
- [Guia completo de testes da Etapa 4](docs/etapas/etapa-4-guia-testes.md).

## Profiles

| Profile | Uso |
| --- | --- |
| Spring `dev` | Aplicacoes executadas localmente; Config Server e bancos acessados por `localhost`. |
| Spring `prod` | Aplicacoes em containers; comunicacao por nomes DNS do Compose. |
| Spring `test` | Testes com PostgreSQL criado pelo Testcontainers e sem Config Server. |
| Spring `native` | Config Server lendo o repositorio de configuracoes do classpath. |
| Compose `dev` | Inicia os dois bancos PostgreSQL e o RabbitMQ. |
| Compose `prod` | Inicia bancos, RabbitMQ, Config Server e as duas aplicacoes. |

### Gerenciamento do schema

- Aplicacao principal em `dev`: `create-drop`; as tabelas sao recriadas e `data-dev.sql` insere um paciente, um medico e um enfermeiro.
- `atendimentos-service` em `dev`: `update`.
- Aplicacoes em `prod`: o Compose fornece `JPA_DDL_AUTO=update` para permitir a primeira execucao academica em bancos vazios.
- Os arquivos de configuracao de `prod` usam `validate` como padrao quando a variavel nao e fornecida.

O `create-drop` da aplicacao principal torna os dados locais de desenvolvimento descartaveis. Os dados de `prod` permanecem nos volumes quando os containers sao reiniciados ou removidos sem `-v`.

## Variaveis de ambiente

O arquivo `.env.example` documenta somente valores utilizados pelo Compose. Copie-o antes da primeira execucao:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Edite `.env` e substitua as senhas ilustrativas antes de subir os servicos. Preserve um `.env` ja configurado. O arquivo real e ignorado pelo Git. Para o envio escolhido neste projeto, configure `MAIL_USERNAME`, `MAIL_PASSWORD` (senha de aplicativo) e `MAIL_FROM` com sua conta de demonstracao. Os testes automatizados dispensam Gmail e `.env`.

| Variavel | Responsabilidade |
| --- | --- |
| `CONFIG_SERVER_PORT` | Porta publicada do Config Server. |
| `SYSHOSPITALAR_SERVER_PORT` | Porta publicada da aplicacao principal. |
| `ATENDIMENTOS_SERVER_PORT` | Porta publicada do servico de atendimentos. |
| `SYSHOSPITALAR_DB_PORT` | Porta local do PostgreSQL principal. |
| `ATENDIMENTOS_DB_PORT` | Porta local do PostgreSQL de atendimentos. |
| `SYSHOSPITALAR_DB_NAME` | Nome do banco principal. |
| `SYSHOSPITALAR_DB_USERNAME` | Usuario do banco principal. |
| `SYSHOSPITALAR_DB_PASSWORD` | Senha local do banco principal. |
| `ATENDIMENTOS_DB_NAME` | Nome do banco de atendimentos. |
| `ATENDIMENTOS_DB_USERNAME` | Usuario do banco de atendimentos. |
| `ATENDIMENTOS_DB_PASSWORD` | Senha local do banco de atendimentos. |
| `RABBITMQ_PORT` / `RABBITMQ_MANAGEMENT_PORT` | Portas locais do broker e do painel. |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Credenciais locais do RabbitMQ. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Conta Gmail e senha de aplicativo. |
| `MAIL_HOST` / `MAIL_PORT` | Servidor e porta SMTP; padroes `smtp.gmail.com` e `587`. |
| `MAIL_FROM` | Remetente da confirmacao. |
| `NOTIFICACOES_CONSUMIDOR_ATIVO` | Ativa ou desativa somente o consumidor. |
| `NOTIFICACOES_*` | Nomes externalizados da exchange, filas e routing keys. |
| `BATCH_INPUT_DIR` | Diretorio temporario dos CSVs. |

### Variaveis recebidas pelas aplicacoes

| Variavel | Ambiente | Responsabilidade |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` e `prod` | Seleciona o profile Spring de cada processo. O Config Server usa `native`. |
| `CONFIG_SERVER_URL` | `dev` e `prod` | Endereco usado pelas duas aplicacoes para obter configuracao centralizada. |
| `ATENDIMENTOS_SERVICE_URL` | `dev` e `prod` | Endereco HTTP usado pela aplicacao principal para acessar o `atendimentos-service`. |
| `SERVER_PORT` | Config Server e `prod` | Porta do Config Server ou porta interna de cada aplicacao no container. |
| `DB_URL` | `prod` | URL JDBC entregue individualmente a cada container de aplicacao. |
| `DB_USERNAME` | `prod` | Usuario do banco pertencente a cada aplicacao. |
| `DB_PASSWORD` | `prod` | Senha do banco pertencente a cada aplicacao. |
| `SYSHOSPITALAR_DB_URL` | `dev` | URL JDBC local do PostgreSQL da aplicacao principal. |
| `SYSHOSPITALAR_DB_USERNAME` | `dev` | Usuario local do banco principal. |
| `SYSHOSPITALAR_DB_PASSWORD` | `dev` | Senha local do banco principal. |
| `ATENDIMENTOS_DB_URL` | `dev` | URL JDBC local do PostgreSQL de atendimentos. |
| `ATENDIMENTOS_DB_USERNAME` | `dev` | Usuario local do banco de atendimentos. |
| `ATENDIMENTOS_DB_PASSWORD` | `dev` | Senha local do banco de atendimentos. |
| `JPA_DDL_AUTO` | `dev` e `prod` | Define a estrategia de schema quando o profile permite sobrescrita. A aplicacao principal fixa `create-drop` em `dev`. |
| `JPA_SHOW_SQL` | `dev` e `prod` | Controla a exibicao das consultas SQL nos logs. |
| `HIBERNATE_FORMAT_SQL` | `dev` e `prod` | Controla a formatacao do SQL exibido nos logs. |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | `dev` e `prod` | Conexao com o broker. Em containers, o host e `rabbitmq`. |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | `dev` e `prod` | Configuracao SMTP; a senha deve ser uma senha de aplicativo. |
| `NOTIFICACOES_CONSUMIDOR_ATIVO` | `dev` e `prod` | Permite interromper o consumidor sem parar API e produtor. |
| `BATCH_INPUT_DIR` | `dev` e `prod` | Diretorio dos arquivos temporarios da importacao. |

Uma aplicacao iniciada pelo Maven nao le `.env` automaticamente. Nesse caso, defina as variaveis de `dev` no terminal ou na configuracao da IDE. Em `prod`, o Compose entrega `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `SERVER_PORT` separadamente para cada container, sem armazenar credenciais no Config Server.

`DB_DRIVER` nao e necessario porque o Spring Boot identifica o driver PostgreSQL pela URL JDBC e pela dependencia instalada. `H2_CONSOLE_ENABLED` tambem nao se aplica mais, pois o H2 foi removido das duas aplicacoes.

## Estrutura relevante

```text
sysHospitalar
|-- src/                         # aplicacao principal
|-- atendimentos-service/        # servico independente
|-- config-server/               # configuracao centralizada
|-- docs/etapas/                 # requisitos, planejamentos e guias academicos
|-- postman/                     # colecoes para demonstracao da API
|-- compose.yml                  # orquestracao dev/prod
|-- .env.example                 # contrato das variaveis locais
`-- Dockerfile                   # imagem da aplicacao principal
```

## Executar o ambiente dev

Todos os comandos partem da raiz do repositorio, em PowerShell. Use terminais separados para os tres processos Java. Os exemplos usam as portas, nomes de bancos e usuarios de `.env.example`; se os alterar, ajuste tambem as URLs locais. Escolha `dev` ou `prod`: eles compartilham os mesmos volumes do Compose e nao sao ambientes isolados. Nao inicie `dev` sobre dados que deseja preservar, pois a aplicacao principal usa `create-drop`.

### Pre-requisitos

- JDK 21 para Maven e execucao local; o Maven Wrapper dispensa instalacao separada do Maven;
- Docker Desktop ou Docker Engine em execucao, com Docker Compose;
- acesso a internet na primeira execucao para baixar dependencias e imagens;
- PowerShell 7 para `Invoke-RestMethod -Form`, ou `curl.exe`/Postman para o upload no Windows PowerShell 5.1;
- portas `5432`, `5433`, `5672`, `15672`, `8888`, `8080` e `8081` livres.

### 1. Iniciar bancos e RabbitMQ

```powershell
docker compose --profile dev up -d
docker compose --profile dev ps
```

Prepare `.env` conforme a secao anterior antes desse comando. Devem iniciar `postgres-principal`, `postgres-atendimentos` e `rabbitmq`; aguarde ficarem saudaveis (`healthy`). O painel fica em [RabbitMQ Management](http://localhost:15672), com usuario e senha definidos no `.env`.

### 2. Iniciar o Config Server

```powershell
$env:SPRING_PROFILES_ACTIVE="native"
$env:SERVER_PORT="8888"
.\mvnw.cmd -f config-server\pom.xml spring-boot:run
```

Validacao:

```powershell
Invoke-RestMethod http://localhost:8888/pedro-carrara-syshospitalar/dev
Invoke-RestMethod http://localhost:8888/atendimentos-service/dev
```

### 3. Iniciar o servico de atendimentos

Em outro terminal:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:CONFIG_SERVER_URL="http://localhost:8888"
$env:ATENDIMENTOS_DB_URL="jdbc:postgresql://localhost:5433/atendimentos"
$env:ATENDIMENTOS_DB_USERNAME="atendimentos"
$env:ATENDIMENTOS_DB_PASSWORD="a-mesma-senha-do-env"
.\mvnw.cmd -f atendimentos-service\pom.xml spring-boot:run
```

### 4. Iniciar a aplicacao principal

Em outro terminal, na raiz:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:CONFIG_SERVER_URL="http://localhost:8888"
$env:ATENDIMENTOS_SERVICE_URL="http://localhost:8081"
$env:SYSHOSPITALAR_DB_URL="jdbc:postgresql://localhost:5432/syshospitalar"
$env:SYSHOSPITALAR_DB_USERNAME="syshospitalar"
$env:SYSHOSPITALAR_DB_PASSWORD="a-mesma-senha-do-env"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="syshospitalar"
$env:RABBITMQ_PASSWORD="a-mesma-senha-do-env"
$env:MAIL_USERNAME="seu-email@gmail.com"
$env:MAIL_PASSWORD="senha-de-aplicativo"
$env:MAIL_FROM="seu-email@gmail.com"
$env:NOTIFICACOES_CONSUMIDOR_ATIVO="true"
.\mvnw.cmd spring-boot:run
```

Use as credenciais correspondentes do `.env` em cada terminal; o Maven nao carrega esse arquivo. Em execucao local, deixe `BATCH_INPUT_DIR` sem definir para usar o diretorio temporario do Java, ou forneca um caminho local gravavel.

Para encerrar bancos e RabbitMQ, depois de parar os tres processos locais com `Ctrl+C`:

```powershell
docker compose --profile dev down
```

## Executar o ambiente prod

O profile `prod` e uma simulacao academica local de producao.

Este e o caminho com todas as aplicacoes em containers: exige Docker e `.env` preparado; o build usa Java 21 e Maven dentro das imagens. Encerre processos locais que ocupem as portas antes de iniciar.

```powershell
docker compose --profile prod config --quiet
docker compose --profile prod up --build -d
docker compose --profile prod ps
```

Devem iniciar seis componentes: dois bancos, RabbitMQ, Config Server, `atendimentos-service` e aplicacao principal. Dentro dos containers, as conexoes utilizam nomes como `config-server`, `atendimentos-service`, `rabbitmq`, `postgres-principal` e `postgres-atendimentos`.

Aguarde os seis servicos ficarem `healthy`. Para acompanhar a inicializacao:

```powershell
docker compose --profile prod logs --tail 100 config-server atendimentos-service aplicacao-principal
Invoke-RestMethod http://localhost:8080/v3/api-docs | Select-Object openapi
Invoke-RestMethod http://localhost:8081/v3/api-docs | Select-Object openapi
```

`config --quiet` valida o Compose sem imprimir os valores das credenciais. Em `prod`, nao ha carga automatica de pacientes e medicos: use o roteiro de demonstracao abaixo. Os healthchecks confirmam inicializacao, mas nao comprovam envio SMTP. Se mudar portas no `.env`, ajuste os enderecos usados no navegador e nas chamadas HTTP.

Parar sem apagar os dados:

```powershell
docker compose --profile prod down
```

O comando `down` preserva os volumes dos bancos e do RabbitMQ. Acrescentar `-v` apaga esses dados; nao e necessario para executar ou demonstrar a etapa.

## Testes

Os testes das duas aplicacoes usam PostgreSQL 17 por Testcontainers. A aplicacao principal tambem usa RabbitMQ real em testes para validar consumo, retentativas e DLQ; o Gmail e sempre simulado.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -f atendimentos-service\pom.xml test
.\mvnw.cmd -f config-server\pom.xml test
```

Execute os tres comandos: o projeto raiz nao e um agregador Maven dos demais. Confirme `BUILD SUCCESS`, `Failures: 0`, `Errors: 0` e `Skipped: 0`. As classes com Testcontainers podem ser ignoradas automaticamente se Docker estiver indisponivel; nesse caso, o sucesso do Maven sozinho nao comprova os requisitos de integracao.

Os relatorios ficam em `target/surefire-reports` de cada aplicacao. Os testes usam containers temporarios, sem utilizar os volumes do Compose. Consulte o [guia de testes](docs/etapas/etapa-4-guia-testes.md) para os cenarios negativos.

## Swagger e OpenAPI

| Aplicacao | Swagger | OpenAPI JSON |
| --- | --- | --- |
| Principal | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| Atendimentos | `http://localhost:8081/swagger-ui.html` | `http://localhost:8081/v3/api-docs` |

## Endpoints principais

### Pacientes

```text
GET    /pacientes
GET    /pacientes/{id}
POST   /pacientes
PUT    /pacientes/{id}
DELETE /pacientes/{id}
GET    /pacientes/filtro?sexo=F
GET    /pacientes/ordenados-por-nome
```

### Medicos

```text
GET    /medicos
GET    /medicos/{id}
POST   /medicos
PUT    /medicos/{id}
DELETE /medicos/{id}
GET    /medicos/filtro?especialidade=Cardiologia
GET    /medicos/ativos
```

### Enfermeiros

```text
GET    /enfermeiros
GET    /enfermeiros/{id}
POST   /enfermeiros
PUT    /enfermeiros/{id}
DELETE /enfermeiros/{id}
GET    /enfermeiros/filtro?setor=UTI
GET    /enfermeiros/ativos
```

### Atendimentos

```text
GET    /atendimentos
GET    /atendimentos/{id}
POST   /atendimentos
PUT    /atendimentos/{id}
DELETE /atendimentos/{id}
GET    /atendimentos/filtro/status?status=ANDAMENTO
GET    /atendimentos/filtro/tipo?tipo=URGENCIA
GET    /atendimentos/ordenados-por-data
```

### Importacao de pacientes

```text
POST /batch/pacientes/importacoes
GET  /batch/pacientes/importacoes/{executionId}
```

O `POST` recebe `multipart/form-data` no campo `arquivo`, devolve `202 Accepted` e um `Location` para consulta. O CSV UTF-8 pode ter ate 2 MB e exige o cabecalho:

```csv
nome,cpf,dataNascimento,sexo,telefone,email,ativo
```

Um exemplo pronto, com linhas validas, invalidas e duplicadas, esta em `src/main/resources/batch/pacientes-exemplo.csv`. Apenas uma importacao pode ficar ativa por vez.

## Demonstracao da mensageria

O fluxo implementado e:

```text
POST /atendimentos
  -> atendimentos-service confirma a persistencia
  -> produtor publica AtendimentoCriadoEvento
  -> RabbitMQ
  -> consumidor
  -> Gmail SMTP
```

O evento transporta somente `eventoId`, `tipoEvento`, `atendimentoId`, `pacienteNome`, `pacienteEmail` e `dataHoraAtendimento`. A resposta HTTP continua sendo `201 Created` sem aguardar o envio do e-mail.

### 1. Preparar paciente e medico

Com o ambiente ativo, configure SMTP com sua conta e use como destinatario uma caixa de e-mail sob seu controle. O paciente inicial de `dev` e ficticio e nao serve para comprovar recebimento. Em PowerShell, substitua o e-mail no exemplo antes de executar:

```powershell
$baseUrl = "http://localhost:8080"
$paciente = Invoke-RestMethod -Method Post -Uri "$baseUrl/pacientes" `
  -ContentType "application/json" -Body (@{
    nome = "Paciente Demonstracao"
    cpf = "48392017654"
    dataNascimento = "1990-05-10"
    sexo = "F"
    telefone = "65999990000"
    email = "SEU-EMAIL-CONTROLADO@gmail.com"
    ativo = $true
  } | ConvertTo-Json)

$medico = Invoke-RestMethod -Method Post -Uri "$baseUrl/medicos" `
  -ContentType "application/json" -Body (@{
    nome = "Medico Demonstracao"
    idade = 40
    cpf = "10987654322"
    email = "medico.demo@example.com"
    ativo = $true
    crm = "CRM-DEMO-4"
    especialidade = "Clinica Geral"
  } | ConvertTo-Json)

$pedido = @{
  dataHoraAtendimento = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
  tipoAtendimento = "URGENCIA"
  statusAtendimento = "ANDAMENTO"
  pacienteId = $paciente.id
  medicoId = $medico.id
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/atendimentos" `
  -ContentType "application/json" -Body $pedido
```

Os tres cadastros devem responder `201 Created`. Reutilize os IDs retornados; nao presuma ID `1` em `prod`. Se repetir o roteiro, consulte os registros existentes em `GET /pacientes` e `GET /medicos`: CPF/e-mail de paciente duplicados retornam `409`.

Nos logs da aplicacao principal, procure `Evento ... publicado`, `Processando evento ...` e `Evento ... processado com sucesso`. Confirme tambem o recebimento do e-mail, cujo conteudo e nome, identificador e data/hora do atendimento. A fila pode esvaziar rapidamente com o consumidor ativo.

### 2. Demonstrar consumidor temporariamente indisponivel

Em `prod`, no terminal que executa o Compose:

```powershell
$env:NOTIFICACOES_CONSUMIDOR_ATIVO="false"
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
docker compose --profile prod ps
```

Aguarde a aplicacao ficar `healthy` e repita apenas o `POST /atendimentos` com `$pedido`. API, produtor, broker e servico de atendimentos continuam disponiveis; o listener fica desativado. No [painel RabbitMQ](http://localhost:15672), abra **Queues and Streams** e a fila `notificacao-email.atendimento-criado`. Confirme **Consumers = 0** e aumento de **Ready**. Nao use **Get messages**, pois essa acao pode retirar a mensagem da fila.

Reative o listener:

```powershell
$env:NOTIFICACOES_CONSUMIDOR_ATIVO="true"
docker compose --profile prod up -d --no-deps --force-recreate aplicacao-principal
docker compose --profile prod logs -f aplicacao-principal
```

Confirme consumidor conectado, reducao de `Ready`, log de sucesso e e-mail recebido. `Ctrl+C` encerra somente o acompanhamento dos logs. Depois, remova a sobrescrita do terminal (`Remove-Item Env:NOTIFICACOES_CONSUMIDOR_ATIVO`) e mantenha `NOTIFICACOES_CONSUMIDOR_ATIVO=true` no `.env`.

Uma variavel do terminal tem precedencia sobre `.env`. Alterar `.env` e executar apenas `docker compose restart` nao atualiza as variaveis do container; por isso o roteiro usa `up --force-recreate`.

Em `dev`, pare a aplicacao principal com `Ctrl+C`, defina a mesma variavel no terminal Maven e inicie-a novamente para cada estado. O `create-drop` apaga e recria pacientes/medicos a cada reinicio: recrie o paciente de demonstracao e use os IDs atuais antes de publicar. Os eventos ja enfileirados carregam os dados necessarios ao consumidor. Para uma demonstracao sem essa recriacao de cadastros, use `prod`.

### 3. Falhas SMTP e limites

O listener faz tres tentativas (a inicial e duas novas, com esperas de 2 e 4 segundos). Se todas falharem, a mensagem segue para `notificacao-email.atendimento-criado.dlq`. O [guia de testes](docs/etapas/etapa-4-guia-testes.md) explica como provocar e restaurar uma falha SMTP. Corrigir a credencial nao reprocessa automaticamente a DLQ.

Se o RabbitMQ falhar durante a publicacao, o produtor trata a falha para preservar o atendimento ja persistido e o `201`. Como esta etapa nao usa transactional outbox nem confirmacao de publicacao pelo broker, a notificacao pode ser perdida; um log de publicacao sozinho nao prova entrega duravel. Mensagens aceitas pelo broker podem ser reentregues: uma falha entre o envio SMTP e o reconhecimento permite e-mail duplicado, pois nao ha controle persistente de idempotencia.

## Demonstracao do Spring Batch

Com a aplicacao principal ativa, envie o [arquivo de exemplo](src/main/resources/batch/pacientes-exemplo.csv) em **PowerShell 7**:

```powershell
$resposta = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/batch/pacientes/importacoes `
  -Form @{ arquivo = Get-Item .\src\main\resources\batch\pacientes-exemplo.csv }

$resposta
```

No Windows PowerShell 5.1, use Postman ou este comando com `curl.exe`, que substitui o upload anterior:

```powershell
$resposta = curl.exe --silent --show-error --fail -X POST `
  -F "arquivo=@src/main/resources/batch/pacientes-exemplo.csv" `
  http://localhost:8080/batch/pacientes/importacoes | ConvertFrom-Json
```

O upload retorna `202 Accepted` e `Location`; isso significa que a execucao foi aceita. Consulte o resultado ate sair de `STARTING`/`STARTED`:

```powershell
do {
  Start-Sleep -Milliseconds 500
  $status = Invoke-RestMethod "http://localhost:8080/batch/pacientes/importacoes/$($resposta.executionId)"
} while ($status.status -in @("STARTING", "STARTED"))
$status | Format-List
Invoke-RestMethod http://localhost:8080/pacientes | Format-Table id,nome,cpf,email
```

| Componente | Implementacao e responsabilidade |
| --- | --- |
| Job | `importacaoPacientesJob`, disparado pelo endpoint, sem execucao automatica na inicializacao. |
| Step | `importarPacientesStep`, com chunk explicitamente configurado em 10. |
| ItemReader | `FlatFileItemReader` com `PacienteCsvLineMapper`: le CSV UTF-8, ignora o cabecalho e exige sete colunas. |
| ItemProcessor | `PacienteImportacaoProcessor`: normaliza nome, CPF, telefone, sexo e e-mail; valida campos e data; filtra CPF/e-mail duplicados no banco ou no arquivo. |
| ItemWriter | `JpaItemWriter`: persiste pacientes no PostgreSQL da aplicacao principal. |

Cada chunk considera ate 10 itens lidos; itens filtrados nao chegam ao writer, e o ultimo bloco pode ser menor. `ativo` vazio assume `true`. O CPF e validado por quantidade de digitos, sem calculo dos digitos verificadores.

| Campo da consulta | Resultado esperado |
| --- | --- |
| `status` | `COMPLETED` quando o processamento termina com sucesso; `FAILED` em falha. |
| `lidos` | Registros entregues pelo reader. |
| `gravados` | Pacientes persistidos. |
| `filtrados` | Registros rejeitados por regra ou duplicidade. |
| `ignorados` | Erros estruturais de leitura tolerados pelo Step. |

Na primeira importacao em um banco sem CPFs/e-mails coincidentes com o exemplo, espere `COMPLETED`, **5 lidos, 2 gravados, 3 filtrados e 0 ignorados**. Ana e Bruno sao os registros gravados. Reenviar o arquivo gera outra execucao e filtra os pacientes ja existentes. Os numeros dependem dos dados anteriores.

Em `dev`, Maria do `data-dev.sql` ja usa o CPF de Ana do CSV: Ana sera filtrada, e a linha chamada `Email Duplicado` podera ser aceita porque o e-mail de Ana nao entrou no conjunto. Confira os dados persistidos, alem dos contadores; os nomes das linhas sao exemplos, nao determinam o resultado da validacao.

Cabecalho invalido, arquivo vazio ou extensao incorreta retornam `400`; execucao inexistente retorna `404`; outra importacao ativa retorna `409`. O limite e 2 MB para o arquivo e tambem para a requisicao multipart completa, portanto reserve espaco para o envelope do upload. Ate 100 erros estruturais podem ser ignorados; o erro seguinte faz o Job falhar. Blocos ja confirmados podem permanecer gravados em uma execucao que falhou.

Ao concluir, o listener tenta excluir o CSV temporario; em falha do Job, preserva-o para diagnostico. No Compose atual, `BATCH_INPUT_DIR` fica no filesystem do container, sem volume: recriar/remover a aplicacao perde esses arquivos, embora os metadados Batch permanecam no PostgreSQL. Uma queda abrupta pode deixar execucao marcada como ativa; nao ha endpoint de recuperacao nesta etapa.

## Reflexão arquitetural da Etapa 4

### 1. Qual operação foi escolhida para comunicação assíncrona?

Escolhi o envio da confirmação de atendimento por e-mail. Após o `atendimentos-service` confirmar a persistência, a aplicação principal publica um evento no RabbitMQ, e o consumidor recebe a mensagem para enviar a confirmação ao paciente.

### 2. Por que essa operação não precisa necessariamente ser concluída durante a requisição original?

O atendimento já foi persistido quando a notificação é publicada. O e-mail comunica esse resultado, mas não determina o sucesso do cadastro. Por isso, o cliente recebe a resposta sem precisar aguardar o envio pelo SMTP.

### 3. O que acontece com a mensagem caso o consumidor esteja temporariamente indisponível?

Com o broker disponível, a mensagem publicada permanece na fila aguardando processamento. Quando o consumidor é reativado, ele recebe a mensagem e realiza o envio. Assim, produtor e consumidor não precisam concluir suas operações no mesmo momento.

### 4. Qual funcionalidade foi escolhida para processamento em lote?

Escolhi a importação de pacientes a partir de um arquivo CSV com múltiplos registros. O Job lê as linhas, aplica as regras de validação e normalização e grava os pacientes válidos no PostgreSQL da aplicação principal.

### 5. Por que essa funcionalidade é adequada para Batch?

A importação exige aplicar o mesmo fluxo de leitura, processamento e escrita a vários registros. Utilizei Spring Batch para organizar esse fluxo em chunks de 10, com transações por bloco, estado da execução e contadores de registros lidos, gravados, filtrados e ignorados.

### 6. Em quais situações da aplicação seria mais adequado utilizar REST, mensageria ou Batch?

No SysHospitalar, utilizo REST quando preciso de uma resposta imediata, mensageria para a comunicação assíncrona entre produtor e consumidor e Batch para o processamento estruturado de conjuntos de dados:

| Mecanismo | Uso concreto | Motivo |
| --- | --- | --- |
| REST | Cadastrar e consultar pacientes; solicitar a criacao do atendimento no `atendimentos-service`. | O cliente ou a aplicacao principal precisa de uma resposta imediata. |
| Mensageria | Enviar a confirmacao de atendimento por e-mail. | O atendimento nao deve depender do tempo ou da disponibilidade do SMTP. |
| Batch | Importar varios pacientes de um CSV. | O conjunto passa pelo mesmo fluxo estruturado de leitura, normalizacao, filtragem e escrita em chunks. |

## Postman

A [colecao da Etapa 4](postman/SysHospitalar-Etapa4.postman_collection.json) contem os fluxos de pacientes, atendimento, inicio do Batch e consulta pelo `executionId`. Ajuste `baseUrl`, troque o e-mail de exemplo por uma caixa sua e selecione localmente o CSV no campo `arquivo`. A colecao preenche `pacienteId` e `executionId` apos respostas de sucesso; `medicoId` deve ser configurado manualmente com um medico existente. A colecao nao cria medicos.

## Exemplo de fluxo funcional

O profile `dev` cria paciente e medico com identificador `1`. Com as duas aplicacoes em execucao, um atendimento pode ser criado pela aplicacao principal:

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

## Validacao e tratamento de erros

Os DTOs de entrada utilizam Bean Validation. Entre as regras existentes estao CPF com 11 numeros, e-mail valido, idade minima de 18 anos para prestadores, data de nascimento nao futura e identificadores relacionados positivos.

As excecoes previstas sao tratadas centralmente. Os principais status sao `200`, `201`, `202`, `204`, `400`, `404`, `409` e `503`. O upload CSV possui validacao propria no service e no processor.

CPF e e-mail de paciente sao unicos na API e no banco. O e-mail e normalizado para minusculas; conflitos de cadastro ou atualizacao retornam `409` sem identificar o outro paciente. Antes de aplicar as constraints em um banco persistente antigo, verifique manualmente CPFs repetidos e e-mails que diferem apenas por maiusculas/minusculas; a aplicacao nao corrige nem exclui registros automaticamente.

## Reflexao arquitetural da Etapa 3

### 1. Quais configuracoes podem variar entre ambientes?

Portas, URLs do Config Server e do servico, URLs JDBC, credenciais, profile ativo, estrategia de schema e exibicao ou formatacao de SQL.

### 2. Quais configuracoes foram externalizadas?

Todas as anteriores foram movidas para variaveis de ambiente, arquivos de profile, repositorio do Config Server ou definicoes do Compose. Nenhuma credencial foi fixada no codigo Java.

### 3. Por que um servico nao deve acessar diretamente o banco de outro?

O acesso direto cria acoplamento ao schema interno, contorna regras de negocio e impede que o servico dono dos dados evolua de forma independente. A integracao deve ocorrer pelo contrato HTTP.

### 4. Qual problema o Docker resolve no projeto?

Padroniza sistema operacional, Java, dependencias e processo de inicializacao, reduzindo diferencas entre computadores e tornando a execucao reproduzivel.

### 5. Qual e a funcao do Docker Compose?

Declarar e coordenar os componentes, suas variaveis, redes, volumes, portas, healthchecks e ordem de inicializacao com um unico comando. Eram cinco componentes na Etapa 3; com RabbitMQ, sao seis na Etapa 4.

### 6. Qual problema uma configuracao centralizada procura resolver?

Evita configuracoes duplicadas ou divergentes entre aplicacoes e ambientes. O Config Server entrega propriedades por nome da aplicacao e profile, enquanto segredos continuam vindo de variaveis de ambiente.

## Historico de validacao da Etapa 3

Na Etapa 3, a validacao da infraestrutura com Docker apresentou os seguintes resultados. Esse historico corresponde a versao anterior a inclusao do RabbitMQ e do Spring Batch:

1. Os profiles `dev` e `prod` do Compose foram validados com `docker compose config`.
2. As imagens da aplicacao principal, do `atendimentos-service` e do Config Server foram construidas sem erros.
3. Os testes das tres aplicacoes passaram; os testes das duas APIs utilizaram PostgreSQL 17 pelo Testcontainers, sem testes ignorados.
4. O ambiente `dev` foi exercitado com volumes isolados: os dois bancos ficaram disponiveis, as aplicacoes carregaram o profile correto pelo Config Server e o fluxo de atendimento funcionou pela aplicacao principal.
5. O ambiente `prod` iniciou os cinco componentes com healthchecks, nomes de servico e bancos independentes.
6. Paciente, medico e atendimento permaneceram disponiveis depois de `docker compose down` e novo `up`, comprovando a persistencia dos volumes.
7. Os registros e volumes temporarios usados na validacao foram removidos, sem apagar os volumes reais de `prod`.
8. Os logs revisados nao expuseram senhas, e o `.env` permaneceu fora do versionamento.

## Validacao e testes da Etapa 4

### Resultados dos testes automatizados

As tres suites terminaram com `BUILD SUCCESS`, totalizando **17 testes, zero falhas, zero erros e zero testes ignorados**. O ambiente dessa execucao utilizou Maven Wrapper, Java 23.0.2 e Docker Desktop com Testcontainers. O alvo de compilacao do projeto e Java 21, tambem utilizado nos Dockerfiles.

| Verificacao | Resultado |
| --- | --- |
| `.\mvnw.cmd test` | 15 testes aprovados, sem falhas, erros ou ignorados. |
| `.\mvnw.cmd -f atendimentos-service\pom.xml test` | 1 teste aprovado, sem falhas, erros ou ignorados. |
| `.\mvnw.cmd -f config-server\pom.xml test` | 1 teste aprovado, sem falhas, erros ou ignorados. |
| `docker compose --env-file .env.example --profile dev config --quiet` | Configuracao valida. |
| `docker compose --env-file .env.example --profile prod config --quiet` | Configuracao valida. |

Na suite de atendimentos, houve um aviso do Surefire ao encerrar a JVM apos o limite de 30 segundos no shutdown. O teste e o build terminaram com sucesso; o comportamento de encerramento permanece como ponto de acompanhamento.

### Funcionalidades e cobertura

| Funcionalidade | Implementacao e cobertura |
| --- | --- |
| Operacao assincrona coerente | Confirmacao de atendimento apos persistencia remota; `AtendimentoServiceNotificacaoTest` verifica ordem e ausencia de publicacao quando a operacao falha. |
| Produtor, fila e consumidor | `AtendimentoCriadoProducer`, topologia duravel em `RabbitMqConfig` e `AtendimentoCriadoConsumer`. |
| Evento proprio e configuracao externa | `AtendimentoCriadoEvento`, propriedades do Config Server, `.env.example` e Compose. |
| Consumidor indisponivel e retomada | `NotificacaoRabbitIntegrationTest` observa mensagem na fila sem consumo e envio ao reativar o listener. |
| Retry e DLQ | Mesmo teste verifica tres chamadas ao SMTP simulado e mensagem real na DLQ do RabbitMQ. |
| Job, Step, reader, processor, writer e chunk | `PacienteBatchConfig` e `PacienteImportacaoProcessor`; os testes exercitam normalizacao, filtros e persistencia no PostgreSQL. |
| Inicio e consulta do Batch | Testes dos endpoints verificam `202`, `Location`, estado final, contadores e `404`. |
| Falha do lote e arquivo temporario | Teste com 101 linhas malformadas verifica `FAILED` e preservacao do arquivo; outro verifica limpeza apos sucesso. |
| Separacao de responsabilidades | Controllers delegam a services; atendimentos persistem no servico independente e pacientes no banco principal. |
| Reflexao arquitetural e execucao | Este README apresenta os tres mecanismos, suas escolhas, limites e roteiros. |

Os testes de mensageria usam RabbitMQ real e `JavaMailSender` simulado; a chamada remota de atendimento e simulada no teste unitario do service. Os testes existentes nao constituem um unico fluxo HTTP ponta a ponta com as tres aplicacoes e Gmail. O teste de importacao valida um bloco com menos de dez registros; a configuracao de chunk 10 esta no codigo, mas a suite atual nao demonstra a passagem por varios chunks.

Os resultados acima nao comprovam o recebimento de e-mail pelo Gmail nem a execucao completa da Etapa 4 pelo Compose. A validacao de `docker compose config --quiet` confere a configuracao, sem construir imagens ou iniciar os servicos. Esses cenarios possuem roteiros proprios de demonstracao.

### Roteiro de demonstracao

Organizei a demonstracao nos seguintes cenarios, com os comandos detalhados nas secoes de [mensageria](#demonstracao-da-mensageria), [Spring Batch](#demonstracao-do-spring-batch) e no [guia de testes](docs/etapas/etapa-4-guia-testes.md):

1. Iniciar o ambiente completo e observar os seis servicos saudaveis.
2. Criar um atendimento e acompanhar a publicacao do evento, o consumo e o recebimento do e-mail em uma caixa controlada.
3. Desativar o consumidor, publicar outro evento e observar a mensagem aguardando na fila; reativar o consumidor e acompanhar o processamento.
4. Simular falha SMTP para observar as retentativas e o encaminhamento para a DLQ.
5. Importar o CSV e consultar o estado final, os contadores e os pacientes gravados, incluindo os cenarios de registros invalidos e duplicados.

Para a demonstracao, utilizo dados ficticios e configuracao local de credenciais. O arquivo `.env` permanece fora do versionamento, e os exemplos nao dependem de dados reais de pacientes.

### Problemas comuns na demonstracao

- **Config Server indisponivel:** inicie-o antes das APIs e confira `CONFIG_SERVER_URL`; a importacao de configuracoes e obrigatoria.
- **Falha de banco/broker apos mudar senha:** volumes existentes mantem as credenciais de inicializacao. Mudar `.env` nao altera automaticamente usuarios ja criados; use a configuracao correspondente ao volume.
- **Fila sem consumidor:** confira `NOTIFICACOES_CONSUMIDOR_ATIVO`, as credenciais do RabbitMQ e os logs. Para mudar variaveis em `prod`, recrie apenas a aplicacao principal conforme o roteiro.
- **Falha SMTP:** confira as variaveis `MAIL_*`; para Gmail, use senha de aplicativo. Mensagens na DLQ exigem inspecao, pois nao ha reprocessamento automatico.
- **`-Form` nao reconhecido:** use PowerShell 7 ou a alternativa `curl.exe`/Postman.
- **Importacao nao termina:** examine logs e conexao com PostgreSQL. Metadados de execucao interrompida exigem analise administrativa; a API nao expoe restart/abandono de Job.

## Uso academico de IA

Utilizei IA como apoio ao planejamento, a implementacao, a revisao e a documentacao deste projeto. A responsabilidade pela revisao do material e pela conferencia com o codigo, os testes e o enunciado permanece minha.
