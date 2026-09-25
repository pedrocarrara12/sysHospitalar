# Atendimentos Service

API REST desenvolvida em Java com Spring Boot para gerenciamento independente dos atendimentos do SysHospitalar.

Este servico foi criado na Etapa 2 para separar a responsabilidade de `Atendimentos` da aplicacao principal. Ele executa em processo proprio, possui banco proprio e e consumido pela aplicacao principal via HTTP.

```text
Cliente HTTP ou aplicacao principal -> Controller -> Service -> Repository -> Banco de Dados
```

## Tecnologias utilizadas

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2 Database
- H2 Console
- SpringDoc OpenAPI / Swagger
- Maven Wrapper

## Responsabilidade do servico

O `atendimentos-service` e responsavel por:

- cadastrar atendimentos;
- consultar atendimentos;
- atualizar atendimentos;
- remover atendimentos;
- filtrar por status;
- filtrar por tipo;
- listar atendimentos ordenados por data e hora;
- persistir os dados de atendimento em banco proprio.

Pacientes, medicos e enfermeiros continuam pertencendo a aplicacao principal. Por isso, este servico armazena apenas os identificadores `pacienteId` e `medicoId`.

## Etapa 2 - Servico Independente

Na Etapa 2, a funcionalidade de atendimentos foi extraida para este projeto para demonstrar comunicacao entre duas aplicacoes Spring Boot.

A aplicacao principal continua expondo os endpoints publicos de atendimento para o cliente. Internamente, ela valida se paciente e medico existem e chama este servico com OpenFeign.

```text
Aplicacao principal
    AtendimentoService
        |
        v
    AtendimentoClient (OpenFeign)
        |
        | HTTP
        v
atendimentos-service
    AtendimentoController
        |
        v
    AtendimentoService
        |
        v
    AtendimentoRepository
        |
        v
    Banco H2 de atendimentos
```

## Entidade principal

### Atendimento

Representa um atendimento realizado para um paciente por um medico.

Campos principais:

- `id`
- `dataHoraAtendimento`
- `tipoAtendimento`
- `statusAtendimento`
- `pacienteId`
- `medicoId`

## Contrato da API

O cadastro e a atualizacao de atendimento recebem apenas os ids relacionados:

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

A resposta do servico tambem retorna ids:

```json
{
  "id": 1,
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

A aplicacao principal e responsavel por enriquecer a resposta publica com `pacienteNome` e `medicoNome`.

## Estrutura do projeto

```text
atendimentos-service
|-- src
|   |-- main
|   |   |-- java
|   |   |   |-- br/com/pedrocarrarafigueiredo/pedro_carrara_syshospitalar
|   |   |   |   |-- atendimento
|   |   |   |   |   |-- controller
|   |   |   |   |   |-- domain
|   |   |   |   |   |-- dto
|   |   |   |   |   |-- enuns
|   |   |   |   |   |-- repository
|   |   |   |   |   |-- service
|   |   |   |   |-- config
|   |   |   |   |-- dto
|   |   |   |   |-- exception
|   |   |-- resources
|   |-- test
|-- pom.xml
```

## Como executar o servico

### Pre-requisitos

- Java 21 ou superior instalado.
- Terminal aberto na pasta `atendimentos-service`.

No Windows, use os comandos com `mvnw.cmd`.

### Rodar os testes

```powershell
.\mvnw.cmd test
```

Esse comando compila o servico e executa os testes automatizados.

### Subir a aplicacao

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrao, o servico sobe em:

```text
http://localhost:8081
```

A porta esta configurada em `src/main/resources/application.properties`:

```properties
server.port=8081
```

## Swagger

Com o servico em execucao, a documentacao da API pode ser acessada em:

```text
http://localhost:8081/swagger-ui.html
```

O arquivo OpenAPI em JSON fica disponivel em:

```text
http://localhost:8081/v3/api-docs
```

## H2 Console

O servico utiliza banco H2 em memoria. Com a aplicacao em execucao, acesse:

```text
http://localhost:8081/h2-console
```

Dados de conexao:

```text
JDBC URL: jdbc:h2:mem:atendimentos_service
User: sa
Password:
```

O campo de senha deve ficar vazio.

## Endpoints principais

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

## Exemplos de requisicao

### Criar atendimento

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

### Filtrar por status

```text
GET /atendimentos/filtro/status?status=ANDAMENTO
```

### Filtrar por tipo

```text
GET /atendimentos/filtro/tipo?tipo=URGENCIA
```

## Validacoes

Os DTOs de request utilizam Bean Validation para validar os dados recebidos pela API.

Exemplos de validacoes:

- Data e hora do atendimento sao obrigatorias.
- Tipo de atendimento e obrigatorio.
- Status do atendimento e obrigatorio.
- `pacienteId` deve ser positivo.
- `medicoId` deve ser positivo.

Quando ocorre erro de validacao, a API retorna `400 Bad Request` com uma resposta padronizada.

## Tratamento de erros

Os erros sao tratados por um `GlobalExceptionHandler`, retornando uma estrutura padronizada:

```json
{
  "localDateTime": "2026-08-30T20:12:07.0835781",
  "status": 400,
  "error": "Bad Request",
  "mensagem": "Mensagem do erro",
  "path": "/atendimentos"
}
```

Principais status utilizados:

- `200 OK`
- `201 Created`
- `204 No Content`
- `400 Bad Request`
- `404 Not Found`
- `409 Conflict`

## Observacoes sobre o banco

O H2 esta configurado em memoria:

```properties
spring.datasource.url=jdbc:h2:mem:atendimentos_service
```

Isso significa que os dados sao apagados quando a aplicacao e encerrada.

## Uso pela aplicacao principal

Para validar a comunicacao da Etapa 2:

1. Suba este servico em `http://localhost:8081`.
2. Suba a aplicacao principal em `http://localhost:8080`.
3. Cadastre paciente e medico pela aplicacao principal.
4. Cadastre um atendimento pela aplicacao principal.
5. Consulte os atendimentos pela aplicacao principal.

Quando este servico esta desligado, a aplicacao principal deve retornar uma resposta controlada com `503 Service Unavailable` nos endpoints de atendimento.
