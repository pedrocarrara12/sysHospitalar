# SysHospitalar

API REST desenvolvida em Java com Spring Boot para gerenciamento basico de um sistema hospitalar.

O projeto permite cadastrar, consultar, atualizar, remover e filtrar dados de pacientes, medicos, enfermeiros e atendimentos. Na Etapa 2, a responsabilidade de `Atendimentos` foi separada para uma aplicacao independente chamada `atendimentos-service`, consumida pela aplicacao principal via HTTP com OpenFeign.

```text
Cliente HTTP -> Controller -> Service -> Feign Client -> atendimentos-service
```

Para pacientes, medicos e enfermeiros, a aplicacao principal continua seguindo a arquitetura:

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
```

## Tecnologias utilizadas

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Cloud OpenFeign
- Bean Validation
- H2 Database
- H2 Console
- SpringDoc OpenAPI / Swagger
- Maven Wrapper

## Funcionalidades

- Cadastro de pacientes.
- Cadastro de medicos.
- Cadastro de enfermeiros.
- Cadastro de atendimentos por meio do `atendimentos-service`.
- Listagem geral dos recursos.
- Busca por identificador.
- Atualizacao de registros.
- Remocao de registros.
- Consultas personalizadas com Spring Data JPA.
- Consultas remotas de atendimentos por status, tipo e ordenacao por data.
- Validacao dos dados recebidos pela API.
- Tratamento padronizado de erros.
- Tratamento controlado de indisponibilidade do servico de atendimentos.
- Documentacao da API via Swagger.

## Etapa 1 - Organizacao Arquitetural

Esta etapa teve como objetivo revisar a organizacao interna da aplicacao antes de qualquer evolucao para microsservicos. O projeto permaneceu como uma unica aplicacao Spring Boot, mas suas responsabilidades foram separadas por dominio.

A arquitetura base esperada foi:

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
```

### Modulos identificados

#### Pacientes

Responsavel pelo cadastro, consulta, atualizacao, remocao, filtros e manutencao dos dados pessoais dos pacientes atendidos pelo sistema.

#### Equipe Clinica

Responsavel pelo cadastro e manutencao dos profissionais de saude. No codigo, essa responsabilidade foi separada em dois pacotes de dominio:

- `medico`: dados de medicos, incluindo especialidade, CRM e status ativo.
- `enfermeiro`: dados de enfermeiros, incluindo setor, COREN e status ativo.

#### Atendimentos

Responsavel pelo registro dos atendimentos realizados, incluindo tipo, status, data e hora, alem do vinculo com paciente e medico.

### Dependencias entre modulos

Um exemplo de dependencia existente e:

```text
Atendimentos -> Pacientes / Medicos
```

Um atendimento precisa consultar um paciente e um medico existentes antes de ser cadastrado ou atualizado. Isso mostra que o modulo de atendimentos depende das informacoes mantidas pelos modulos de pacientes e equipe clinica.

### Candidato a servico independente

O modulo de `Atendimentos` foi escolhido como candidato a servico independente.

Responsabilidade:

- Registrar e manter o ciclo de vida dos atendimentos hospitalares.
- Controlar tipo, status, data e hora do atendimento.
- Relacionar o atendimento aos identificadores de paciente e medico.

Motivo para separacao futura:

- Possui fluxo operacional proprio.
- Depende de outros modulos por identificadores claros.
- Pode evoluir com regras especificas, como historico de atendimento, agenda, triagem ou integracao com outros sistemas.

## Etapa 2 - Servico Independente de Atendimentos

Na Etapa 2, a responsabilidade de `Atendimentos` foi extraida da aplicacao principal para o projeto `atendimentos-service`.

### Servico independente

O `atendimentos-service` e uma aplicacao Spring Boot separada, localizada na pasta:

```text
atendimentos-service
```

Ele e responsavel por:

- cadastrar atendimentos;
- consultar atendimentos;
- atualizar atendimentos;
- remover atendimentos;
- filtrar atendimentos por status;
- filtrar atendimentos por tipo;
- listar atendimentos ordenados por data e hora;
- persistir os dados de atendimento em banco proprio.

### Comunicacao entre aplicacoes

A aplicacao principal continua expondo os endpoints publicos de atendimento, mas nao persiste mais atendimentos diretamente. Ela valida os dados locais de paciente e medico e chama o `atendimentos-service` por HTTP usando OpenFeign.

```text
Cliente HTTP
    |
    v
Aplicacao principal
    AtendimentoController
        |
        v
    AtendimentoService
        |
        | valida paciente e medico no banco local
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

### Configuracao da URL do servico

O endereco do `atendimentos-service` fica externalizado em `src/main/resources/application.properties`:

```properties
services.atendimento.url=${ATENDIMENTOS_SERVICE_URL:http://localhost:8081}
```

Por padrao, a aplicacao principal chama:

```text
http://localhost:8081
```

Em outro ambiente, a URL pode ser alterada com a variavel de ambiente `ATENDIMENTOS_SERVICE_URL`, sem recompilar o projeto.

### Contrato publico

Os endpoints publicos de atendimentos permanecem disponiveis na aplicacao principal:

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

O `atendimentos-service` trabalha com `pacienteId` e `medicoId`. A aplicacao principal enriquece a resposta publica com `pacienteNome` e `medicoNome`, consultando seus dados locais.

### Persistencia

Somente o `atendimentos-service` persiste atendimentos.

A aplicacao principal mantem persistencia local apenas para:

- pacientes;
- medicos;
- enfermeiros.

### Indisponibilidade do servico

Quando o `atendimentos-service` esta indisponivel, a aplicacao principal converte a falha de comunicacao em uma resposta controlada:

```text
503 Service Unavailable
```

Exemplo de resposta:

```json
{
  "localDateTime": "2026-09-25T14:00:00",
  "status": 503,
  "error": "Service Unavailable",
  "mensagem": "Servico de atendimentos temporariamente indisponivel",
  "path": "/atendimentos"
}
```

Assim, detalhes internos do Feign, endereco remoto ou stack trace nao sao expostos ao cliente.

### Reflexao arquitetural

#### Qual funcionalidade foi separada da aplicacao principal?

A funcionalidade de gerenciamento de atendimentos foi separada para o `atendimentos-service`.

#### Por que ela foi escolhida?

Porque `Atendimentos` possui um ciclo operacional proprio e ja dependia de pacientes e medicos por identificadores claros. Isso facilita a separacao sem transformar todo o sistema em microsservicos.

#### O que ficou mais complexo depois da separacao?

A comunicacao ficou mais complexa porque a aplicacao principal agora depende de uma chamada HTTP. Tambem passou a ser necessario tratar indisponibilidade, configurar URL externa e validar o contrato entre as duas aplicacoes.

#### O que acontece com a funcionalidade principal quando o novo servico fica indisponivel?

Os endpoints de atendimento da aplicacao principal retornam uma resposta controlada com status `503 Service Unavailable`. As funcionalidades de pacientes, medicos e enfermeiros continuam funcionando na aplicacao principal.

#### A funcionalidade realmente precisa permanecer independente ou poderia continuar na aplicacao principal?

Para um sistema pequeno, ela poderia continuar na aplicacao principal. A separacao foi feita como exercicio arquitetural da Etapa 2 e faz sentido como preparacao para cenarios em que atendimentos tenham regras, carga ou evolucao propria.

## Entidades principais

### Paciente

Representa o paciente atendido pelo sistema.

Campos principais:

- `id`
- `nome`
- `cpf`
- `dataNascimento`
- `sexo`
- `telefone`
- `email`
- `ativo`

### Medico

Representa um prestador do tipo medico.

Campos principais:

- `id`
- `nome`
- `idade`
- `cpf`
- `email`
- `ativo`
- `crm`
- `especialidade`

### Enfermeiro

Representa um prestador do tipo enfermeiro.

Campos principais:

- `id`
- `nome`
- `idade`
- `cpf`
- `email`
- `ativo`
- `coren`
- `setor`

### Atendimento

Representa um atendimento realizado para um paciente por um medico.

Na aplicacao principal, atendimento e exposto por DTOs e consumido do `atendimentos-service`. A entidade JPA de atendimento pertence ao servico independente.

Campos principais:

- `id`
- `dataHoraAtendimento`
- `tipoAtendimento`
- `statusAtendimento`
- `pacienteId`
- `pacienteNome`
- `medicoId`
- `medicoNome`

## Relacionamentos

O atendimento usa os identificadores de paciente e medico:

- muitos atendimentos podem pertencer a um paciente;
- muitos atendimentos podem estar associados a um medico.

O `atendimentos-service` armazena apenas `pacienteId` e `medicoId`. A aplicacao principal valida se esses identificadores existem antes de criar ou atualizar um atendimento.

Na API, o cadastro de atendimento recebe:

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

## Estrutura do projeto

```text
sysHospitalar
|-- src
|   |-- main
|   |   |-- java
|   |   |   |-- br/com/pedrocarrarafigueiredo/pedro_carrara_syshospitalar
|   |   |   |   |-- atendimento
|   |   |   |   |   |-- client
|   |   |   |   |   |-- controller
|   |   |   |   |   |-- dto
|   |   |   |   |   |-- enuns
|   |   |   |   |   |-- service
|   |   |   |   |-- config
|   |   |   |   |-- domain
|   |   |   |   |-- dto
|   |   |   |   |-- enfermeiro
|   |   |   |   |-- exception
|   |   |   |   |-- medico
|   |   |   |   |-- paciente
|   |   |-- resources
|-- atendimentos-service
|   |-- src
|   |-- pom.xml
|-- docs
|-- pom.xml
```

Na aplicacao principal, o pacote `atendimento` contem o controller publico, o service de orquestracao, DTOs, enums e o `AtendimentoClient` Feign. A persistencia de atendimentos fica no projeto `atendimentos-service`.

## Como executar o projeto

### Pre-requisitos

- Java 21 ou superior instalado.
- Terminal aberto na pasta raiz do projeto.
- Dois terminais para executar a aplicacao principal e o `atendimentos-service` ao mesmo tempo.

No Windows, use os comandos com `mvnw.cmd`.

### Rodar os testes da aplicacao principal

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```

### Rodar os testes do atendimentos-service

Na pasta `atendimentos-service`:

```powershell
cd atendimentos-service
.\mvnw.cmd test
```

### Subir o atendimentos-service

No primeiro terminal:

```powershell
cd atendimentos-service
.\mvnw.cmd spring-boot:run
```

Por padrao, o servico sobe em:

```text
http://localhost:8081
```

### Subir a aplicacao principal

No segundo terminal, na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrao, a aplicacao principal sobe em:

```text
http://localhost:8080
```

Se o `atendimentos-service` estiver em outra URL, configure a variavel de ambiente antes de subir a aplicacao principal:

```powershell
$env:ATENDIMENTOS_SERVICE_URL="http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

## Swagger

Com as aplicacoes em execucao, a documentacao da aplicacao principal pode ser acessada em:

```text
http://localhost:8080/swagger-ui.html
```

A documentacao do `atendimentos-service` pode ser acessada em:

```text
http://localhost:8081/swagger-ui.html
```

O arquivo OpenAPI em JSON da aplicacao principal fica disponivel em:

```text
http://localhost:8080/v3/api-docs
```

O arquivo OpenAPI em JSON do `atendimentos-service` fica disponivel em:

```text
http://localhost:8081/v3/api-docs
```

## H2 Console

A aplicacao principal utiliza banco H2 em memoria para pacientes, medicos e enfermeiros:

```text
http://localhost:8080/h2-console
```

Dados de conexao:

```text
JDBC URL: jdbc:h2:mem:syshospitalar
User: sa
Password:
```

O `atendimentos-service` utiliza outro banco H2 em memoria para atendimentos:

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

Os endpoints abaixo continuam expostos pela aplicacao principal, mas sao processados pelo `atendimentos-service`:

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

### Criar paciente

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "dataNascimento": "1990-05-10",
  "sexo": "F",
  "telefone": "65999990000",
  "email": "maria@email.com",
  "ativo": true
}
```

### Criar medico

```json
{
  "nome": "Joao Medico",
  "idade": 40,
  "cpf": "10987654321",
  "email": "joao@email.com",
  "ativo": true,
  "crm": "CRM123",
  "especialidade": "Cardiologia"
}
```

### Criar enfermeiro

```json
{
  "nome": "Ana Enfermeira",
  "idade": 32,
  "cpf": "11122233344",
  "email": "ana@email.com",
  "ativo": true,
  "coren": "COREN123",
  "setor": "UTI"
}
```

### Criar atendimento

Antes de criar um atendimento pela aplicacao principal, cadastre pelo menos um paciente e um medico. O `atendimentos-service` tambem deve estar em execucao.

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

## Validacoes

Os DTOs de request utilizam Bean Validation para validar os dados recebidos pela API.

Exemplos de validacoes:

- Campos obrigatorios com `@NotBlank` e `@NotNull`.
- CPF com exatamente 11 numeros.
- E-mail em formato valido.
- Idade minima de 18 anos para prestadores.
- Data de nascimento nao pode ser futura.
- Ids relacionados devem ser positivos.
- Paciente e medico precisam existir na aplicacao principal antes do cadastro ou atualizacao de atendimento.

Quando ocorre erro de validacao, a API retorna `400 Bad Request` com uma resposta padronizada.

## Tratamento de erros

Os erros sao tratados por um `GlobalExceptionHandler`, retornando uma estrutura padronizada:

```json
{
  "localDateTime": "2026-08-30T20:12:07.0835781",
  "status": 400,
  "error": "Bad Request",
  "mensagem": "Mensagem do erro",
  "path": "/pacientes"
}
```

Principais status utilizados:

- `200 OK`
- `201 Created`
- `204 No Content`
- `400 Bad Request`
- `404 Not Found`
- `409 Conflict`
- `503 Service Unavailable`

## Roteiro de validacao da Etapa 2

1. Subir o `atendimentos-service` em `http://localhost:8081`.
2. Subir a aplicacao principal em `http://localhost:8080`.
3. Criar um paciente pela aplicacao principal.
4. Criar um medico pela aplicacao principal.
5. Criar um atendimento pela aplicacao principal usando `pacienteId` e `medicoId`.
6. Consultar `/atendimentos` pela aplicacao principal e confirmar a resposta com `pacienteNome` e `medicoNome`.
7. Parar o `atendimentos-service`.
8. Chamar `/atendimentos` pela aplicacao principal e confirmar o retorno `503 Service Unavailable`.
9. Executar `.\mvnw.cmd test` na aplicacao principal.
10. Executar `.\mvnw.cmd test` dentro de `atendimentos-service`.

## Observacoes sobre os bancos

Os bancos H2 estao configurados em memoria.

Aplicacao principal:

```properties
spring.datasource.url=jdbc:h2:mem:syshospitalar
```

Servico de atendimentos:

```properties
spring.datasource.url=jdbc:h2:mem:atendimentos_service
```

Isso significa que os dados sao apagados quando cada aplicacao e encerrada.

## Marco da Etapa 2

A tag `etapa-2` deve ser criada somente depois da implementacao, dos testes de comunicacao e da revisao final.
