# SysHospitalar

API REST em Java e Spring Boot para gerenciamento de pacientes, medicos, enfermeiros e atendimentos hospitalares.

A aplicacao principal mantem os cadastros locais e expoe a API publica. A responsabilidade de atendimentos pertence ao `atendimentos-service`, consumido por HTTP com OpenFeign. Na Etapa 4, a criacao de um atendimento publica uma notificacao assincrona no RabbitMQ e a aplicacao principal tambem oferece importacao de pacientes por CSV com Spring Batch.

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
    | HTTP / OpenFeign
    v
atendimentos-service -------------------> PostgreSQL de atendimentos
    ^
    |
    +-- confirmacao -- Aplicacao principal -- evento --> RabbitMQ
                                                   |
                                                   `--> consumidor --> Gmail SMTP

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

### Etapa 2 - servico independente

Atendimentos foi separado como uma aplicacao Spring Boot independente. A aplicacao principal continua expondo o contrato publico e valida a existencia local do paciente e do medico antes de chamar o servico. Falhas de comunicacao sao convertidas em `503 Service Unavailable`, sem expor detalhes internos.

### Etapa 3 - configuracao e execucao

A solucao passou a utilizar:

- profiles Spring `dev`, `prod`, `test` e `native`;
- configuracao centralizada por Config Server;
- dois bancos PostgreSQL com credenciais e volumes independentes;
- variaveis de ambiente para URLs, portas e credenciais;
- um Dockerfile por aplicacao executavel;
- um `compose.yml` com profiles `dev` e `prod`, redes, volumes e healthchecks.

### Etapa 4 - assincrono e lote

Depois que o `atendimentos-service` confirma a persistencia, a aplicacao principal publica um `AtendimentoCriadoEvento`. O consumidor envia uma confirmacao simples ao e-mail do paciente, sem dados clinicos. Falhas SMTP recebem tres tentativas e, depois disso, seguem para uma DLQ.

A importacao Batch recebe CSV de pacientes, normaliza e valida cada linha e grava registros validos no PostgreSQL principal em chunks de 10. O inicio e a consulta da execucao sao expostos por endpoints separados.

Documentação aprofundada:

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
Copy-Item .env.example .env
```

Substitua as senhas ilustrativas. O `.env` real e ignorado pelo Git.

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

### Pre-requisitos

- Java 21;
- Docker Desktop ou Docker Engine com Compose;
- portas `5432`, `5433`, `5672`, `15672`, `8888`, `8080` e `8081` livres.

### 1. Iniciar bancos e RabbitMQ

```powershell
docker compose --profile dev up -d
docker compose --profile dev ps
```

Devem iniciar `postgres-principal`, `postgres-atendimentos` e `rabbitmq`. O painel fica em `http://localhost:15672`.

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
Set-Location atendimentos-service
.\mvnw.cmd spring-boot:run
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
$env:RABBITMQ_USERNAME="syshospitalar"
$env:RABBITMQ_PASSWORD="a-mesma-senha-do-env"
$env:MAIL_USERNAME="seu-email@gmail.com"
$env:MAIL_PASSWORD="senha-de-aplicativo"
$env:MAIL_FROM="seu-email@gmail.com"
.\mvnw.cmd spring-boot:run
```

Para encerrar somente os bancos, depois de parar as aplicacoes locais:

```powershell
docker compose --profile dev down
```

## Executar o ambiente prod

O profile `prod` e uma simulacao academica local de producao.

```powershell
docker compose --profile prod config
docker compose --profile prod up --build -d
docker compose --profile prod ps
```

Devem iniciar seis componentes: dois bancos, RabbitMQ, Config Server, `atendimentos-service` e aplicacao principal. Dentro dos containers, as conexoes utilizam nomes como `config-server`, `atendimentos-service`, `rabbitmq`, `postgres-principal` e `postgres-atendimentos`.

Parar sem apagar os dados:

```powershell
docker compose --profile prod down
```

Apagar containers e volumes, somente quando os dados puderem ser descartados:

```powershell
docker compose --profile prod down -v
```

## Testes

Os testes das duas aplicacoes usam PostgreSQL 17 por Testcontainers. A aplicacao principal tambem usa RabbitMQ real em testes para validar consumo, retentativas e DLQ; o Gmail e sempre simulado.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -f atendimentos-service\pom.xml test
.\mvnw.cmd -f config-server\pom.xml test
```

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

Para observar o desacoplamento:

1. Configure Gmail com uma senha de aplicativo e crie um atendimento; confirme a mensagem recebida.
2. Reinicie apenas a aplicacao principal com `NOTIFICACOES_CONSUMIDOR_ATIVO=false`.
3. Crie outro atendimento e abra `http://localhost:15672`.
4. Verifique uma mensagem aguardando em `notificacao-email.atendimento-criado`.
5. Reinicie a aplicacao com `NOTIFICACOES_CONSUMIDOR_ATIVO=true`.
6. Confirme que a fila foi consumida e o e-mail foi enviado.
7. Para demonstrar falha, use temporariamente uma configuracao SMTP invalida: ocorrem tres tentativas, com esperas de 2 e 4 segundos, e a mensagem termina em `notificacao-email.atendimento-criado.dlq`.

Se o RabbitMQ falhar durante a publicacao, o atendimento ja persistido e o `201` sao preservados e a falha e registrada por IDs seguros. Como esta etapa nao usa transactional outbox, essa notificacao pode ser perdida. A entrega do RabbitMQ e pelo menos uma vez; portanto, em uma falha rara entre o SMTP e o reconhecimento da mensagem, um e-mail pode ser duplicado.

## Demonstracao do Spring Batch

Com a aplicacao principal ativa, envie o arquivo de exemplo:

```powershell
$resposta = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/batch/pacientes/importacoes `
  -Form @{ arquivo = Get-Item .\src\main\resources\batch\pacientes-exemplo.csv }

Invoke-RestMethod "http://localhost:8080/batch/pacientes/importacoes/$($resposta.executionId)"
```

O reader le o CSV, o processor normaliza nome, CPF, telefone, sexo e e-mail, valida data e campos e filtra duplicidades. O writer persiste pacientes validos no PostgreSQL principal em chunks de 10. Arquivos de Jobs concluidos sao excluidos; arquivos de Jobs que falham sao preservados para diagnostico.

## REST, mensageria e Batch no SysHospitalar

| Mecanismo | Uso concreto | Motivo |
| --- | --- | --- |
| REST | Cadastrar e consultar pacientes; solicitar a criacao do atendimento no `atendimentos-service`. | O cliente ou a aplicacao principal precisa de uma resposta imediata. |
| Mensageria | Enviar a confirmacao de atendimento por e-mail. | O atendimento nao deve depender do tempo ou da disponibilidade do SMTP. |
| Batch | Importar varios pacientes de um CSV. | O conjunto passa pelo mesmo fluxo estruturado de leitura, normalizacao, filtragem e escrita em chunks. |

O envio de e-mail foi escolhido para a operacao assincrona porque e um efeito posterior: ele informa algo que ja foi persistido, mas nao determina o sucesso do atendimento. A importacao foi escolhida para Batch porque processa multiplos registros e precisa de contadores, estado da execucao e transacoes por bloco.

## Postman

A colecao `postman/SysHospitalar-Etapa4.postman_collection.json` contem os fluxos de pacientes, atendimento, inicio do Batch e consulta pelo `executionId`. No upload, selecione localmente o arquivo CSV antes de enviar.

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

As excecoes sao tratadas centralmente. Os principais status sao `200`, `201`, `204`, `400`, `404`, `409` e `503`.

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

Declarar e coordenar os cinco componentes, suas variaveis, redes, volumes, portas, healthchecks e ordem de inicializacao com um unico comando.

### 6. Qual problema uma configuracao centralizada procura resolver?

Evita configuracoes duplicadas ou divergentes entre aplicacoes e ambientes. O Config Server entrega propriedades por nome da aplicacao e profile, enquanto segredos continuam vindo de variaveis de ambiente.

## Validacao concluida da Etapa 3

A configuracao e a execucao da Etapa 3 foram verificadas com Docker disponivel:

1. Os profiles `dev` e `prod` do Compose foram validados com `docker compose config`.
2. As imagens da aplicacao principal, do `atendimentos-service` e do Config Server foram construidas sem erros.
3. Os testes das tres aplicacoes passaram; os testes das duas APIs utilizaram PostgreSQL 17 pelo Testcontainers, sem testes ignorados.
4. O ambiente `dev` foi exercitado com volumes isolados: os dois bancos ficaram disponiveis, as aplicacoes carregaram o profile correto pelo Config Server e o fluxo de atendimento funcionou pela aplicacao principal.
5. O ambiente `prod` iniciou os cinco componentes com healthchecks, nomes de servico e bancos independentes.
6. Paciente, medico e atendimento permaneceram disponiveis depois de `docker compose down` e novo `up`, comprovando a persistencia dos volumes.
7. Os registros e volumes temporarios usados na validacao foram removidos, sem apagar os volumes reais de `prod`.
8. Os logs revisados nao expuseram senhas, e o `.env` permaneceu fora do versionamento.

A tag `etapa-3` nao e criada automaticamente. Depois de revisar o codigo, a documentacao e os testes, o aluno deve cria-la manualmente quando considerar a entrega finalizada.

## Uso academico de IA

Este projeto utilizou IA como apoio a revisao, planejamento, implementacao e documentacao. Todo resultado deve ser revisado pelo aluno e conferido contra o codigo, os testes e o enunciado antes da entrega.
