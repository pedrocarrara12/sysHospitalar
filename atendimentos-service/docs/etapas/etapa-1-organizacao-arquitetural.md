# Etapa 1 - Organizacao Arquitetural

Este arquivo serve como roteiro de apoio para implementar a Etapa 1 do projeto SysHospitalar.

O objetivo desta etapa e revisar e organizar a aplicacao atual, mantendo-a como uma unica aplicacao Spring Boot. Nao criar microsservicos, nao separar projetos e nao implementar comunicacao entre servicos nesta etapa.

## Objetivo

- Revisar a estrutura interna da aplicacao.
- Garantir separacao adequada entre controller, service e repository.
- Organizar os pacotes por dominio ou funcionalidade.
- Documentar modulos, dependencias e candidato futuro a servico independente.

## Arquitetura esperada

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
```

Regras de separacao:

- Controllers devem cuidar da comunicacao HTTP.
- Services devem concentrar regras e operacoes da aplicacao.
- Repositories devem cuidar do acesso aos dados.
- Controllers nao devem acessar repositories diretamente.
- Regras de negocio nao devem ficar concentradas nos controllers.

## Estrategia sugerida

A reorganizacao pode ser gradual por dominio, comecando por `Atendimentos`.

Motivo:

- `Atendimentos` representa uma responsabilidade clara do sistema.
- Depende de `Pacientes` e `Medicos`, mostrando bem o acoplamento entre modulos.
- Ja foi identificado como candidato futuro a servico independente.

## Primeiro bloco - Modulo Atendimentos

Status: concluido.

A estrutura de pacotes ficou equivalente a:

```text
atendimento
|-- controller
|-- service
|-- repository
|-- domain
|-- dto
|   |-- request
|   |-- response
|-- enuns
```

Arquivos ligados diretamente ao modulo:

- `AtendimentoController`
- `AtendimentoService`
- `AtendimentoRepository`
- `Atendimento`
- `AtendimentoRequest`
- `AtendimentoResponse`
- `StatusAtendimento`
- `TipoAtendimento`

Ao mover esses arquivos:

- Ajustar apenas imports e declaracoes de package.
- Manter endpoints iguais.
- Manter JSON de entrada e saida igual.
- Manter regras de negocio iguais.
- Manter `AtendimentoService` consultando `PacienteRepository` e `MedicoRepository`.

## Blocos seguintes - Paciente, Medico e Enfermeiro

Status: concluido.

Os demais dominios tambem foram organizados em pacotes proprios, mantendo a aplicacao como um unico monolito Spring Boot:

```text
paciente
|-- controller
|-- service
|-- repository
|-- domain
|-- dto
|   |-- request
|   |-- response

medico
|-- controller
|-- service
|-- repository
|-- domain
|-- dto
|   |-- request
|   |-- response

enfermeiro
|-- controller
|-- service
|-- repository
|-- domain
|-- dto
|   |-- request
|   |-- response
```

Pacotes e classes compartilhadas mantidos nesta etapa:

- `config`
- `exception`
- `dto.mapper.HospitalMapper`
- `dto.ErrorResponse`
- `domain.Prestador`

Esses itens nao foram separados para evitar refatoracao maior que o necessario na Etapa 1.

## Contratos que nao devem mudar

Endpoints de atendimentos:

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

Contrato de cadastro de atendimento:

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

## README

O README deve registrar:

- Pelo menos tres modulos ou responsabilidades do sistema.
- Responsabilidade de cada modulo.
- Um exemplo de dependencia entre modulos.
- Um candidato a servico independente futuro.
- Motivo pelo qual esse candidato poderia ser separado.
- Quais partes da aplicacao dependem dele atualmente.

Sugestao atual:

- `Pacientes`: cadastro e manutencao dos dados dos pacientes.
- `Medicos`: cadastro e manutencao dos dados dos medicos.
- `Enfermeiros`: cadastro e manutencao dos dados dos enfermeiros.
- `Atendimentos`: registro e acompanhamento dos atendimentos.
- Dependencia: `Atendimentos -> Pacientes / Medicos`.
- Candidato futuro: `Atendimentos`.

## O que nao fazer nesta etapa

- Nao criar outro projeto Spring Boot.
- Nao criar microsservico.
- Nao implementar Feign Client.
- Nao adicionar Docker, Config Server, RabbitMQ ou Spring Batch.
- Nao trocar H2 por outro banco ainda.
- Nao criar tag `etapa-1` antes da revisao final.

## Checklist de validacao

- Controllers continuam sem acesso direto aos repositories.
- Services concentram regras e operacoes da aplicacao.
- Repositories continuam responsaveis pelo acesso aos dados.
- DTOs de entrada usam Bean Validation.
- Excecoes sao tratadas de forma centralizada.
- Existem pelo menos duas consultas Spring Data relacionadas ao dominio.
- Swagger documenta os endpoints principais.
- README apresenta modulos, dependencias e candidato a servico independente.
- A aplicacao compila e os testes passam com:

```powershell
.\mvnw.cmd test
```

## Marco esperado

Ao concluir e revisar a Etapa 1, registrar a tag:

```text
etapa-1
```
