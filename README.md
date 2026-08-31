# SysHospitalar

API REST desenvolvida em Java com Spring Boot para gerenciamento basico de um sistema hospitalar.

O projeto permite cadastrar, consultar, atualizar, remover e filtrar dados de pacientes, medicos, enfermeiros e atendimentos. A aplicacao foi evoluida para utilizar persistencia com Spring Data JPA e banco de dados H2 em memoria, seguindo a arquitetura:

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
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

## Funcionalidades

- Cadastro de pacientes.
- Cadastro de medicos.
- Cadastro de enfermeiros.
- Cadastro de atendimentos vinculados a paciente e medico.
- Listagem geral dos recursos.
- Busca por identificador.
- Atualizacao de registros.
- Remocao de registros.
- Consultas personalizadas com Spring Data JPA.
- Validacao dos dados recebidos pela API.
- Tratamento padronizado de erros.
- Documentacao da API via Swagger.

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

Campos principais:

- `id`
- `dataHoraAtendimento`
- `tipoAtendimento`
- `statusAtendimento`
- `paciente`
- `medico`

## Relacionamentos

O atendimento possui relacionamento muitos-para-um com paciente e medico:

- Muitos atendimentos podem pertencer a um paciente.
- Muitos atendimentos podem estar associados a um medico.

Na API, o cadastro de atendimento recebe apenas os ids relacionados:

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
src/main/java/br/com/pedrocarrarafigueiredo/pedro_carrara_syshospitalar
|-- config
|-- controller
|-- domain
|-- dto
|   |-- mapper
|   |-- request
|   |-- response
|-- enuns
|-- exception
|-- repository
|-- service
```

## Como executar o projeto

### Pre-requisitos

- Java 21 ou superior instalado.
- Terminal aberto na pasta raiz do projeto.

No Windows, use os comandos com `mvnw.cmd`.

### Rodar os testes

```powershell
.\mvnw.cmd test
```

Esse comando compila o projeto e executa os testes automatizados. Como o projeto possui teste com `@SpringBootTest`, o contexto do Spring e o banco H2 sao inicializados temporariamente durante os testes.

### Subir a aplicacao

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrao, a aplicacao sobe em:

```text
http://localhost:8080
```

Se a porta `8080` estiver ocupada, execute em outra porta:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Nesse caso, acesse:

```text
http://localhost:8081
```

## Swagger

Com a aplicacao em execucao, a documentacao da API pode ser acessada em:

```text
http://localhost:8080/swagger-ui.html
```

Ou, caso esteja usando a porta `8081`:

```text
http://localhost:8081/swagger-ui.html
```

O arquivo OpenAPI em JSON fica disponivel em:

```text
http://localhost:8080/v3/api-docs
```

## H2 Console

O projeto utiliza banco H2 em memoria. Com a aplicacao em execucao, acesse:

```text
http://localhost:8080/h2-console
```

Dados de conexao:

```text
JDBC URL: jdbc:h2:mem:syshospitalar
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

Antes de criar um atendimento, cadastre pelo menos um paciente e um medico.

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

## Observacoes sobre o banco

O H2 esta configurado em memoria:

```properties
spring.datasource.url=jdbc:h2:mem:syshospitalar
```

Isso significa que os dados sao apagados quando a aplicacao e encerrada.
