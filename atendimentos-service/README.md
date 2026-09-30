# Atendimentos Service

Aplicacao Spring Boot independente responsavel pelo cadastro e ciclo de vida dos atendimentos do SysHospitalar.

## Responsabilidade

O servico cadastra, consulta, atualiza, remove e filtra atendimentos. Ele persiste somente:

- data e hora;
- tipo e status;
- identificadores do paciente e do medico.

O servico nao acessa o banco da aplicacao principal. A existencia de paciente e medico e validada pela aplicacao principal antes da chamada HTTP.

```text
Aplicacao principal -> OpenFeign -> AtendimentoController
                                      |
                                      v
                              AtendimentoService
                                      |
                                      v
                            AtendimentoRepository
                                      |
                                      v
                          PostgreSQL de atendimentos
```

## Tecnologias

- Java 21 e Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA e Bean Validation
- Spring Cloud Config Client
- PostgreSQL
- SpringDoc OpenAPI / Swagger
- Testcontainers PostgreSQL
- Docker

## Profiles e configuracao

| Profile | Comportamento |
| --- | --- |
| `dev` | Config Server em `localhost:8888`, datasource recebido por variaveis `ATENDIMENTOS_DB_*` e schema em `update`. |
| `prod` | Config Server e PostgreSQL acessados pelos nomes dos servicos do Compose. |
| `test` | Config Server desabilitado e PostgreSQL temporario fornecido pelo Testcontainers. |

Variaveis utilizadas na execucao local:

| Variavel | Exemplo dev |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `CONFIG_SERVER_URL` | `http://localhost:8888` |
| `ATENDIMENTOS_DB_URL` | `jdbc:postgresql://localhost:5433/atendimentos` |
| `ATENDIMENTOS_DB_USERNAME` | `atendimentos` |
| `ATENDIMENTOS_DB_PASSWORD` | valor definido no `.env` |
| `ATENDIMENTOS_SERVER_PORT` | `8081` |

Em `prod`, o Compose converte essas configuracoes para `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `SERVER_PORT` dentro do container.

## Executar localmente

Antes de iniciar o servico:

1. inicie os bancos com `docker compose --profile dev up -d` na raiz;
2. inicie o Config Server com profile `native`;
3. defina as variaveis abaixo.

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:CONFIG_SERVER_URL="http://localhost:8888"
$env:ATENDIMENTOS_DB_URL="jdbc:postgresql://localhost:5433/atendimentos"
$env:ATENDIMENTOS_DB_USERNAME="atendimentos"
$env:ATENDIMENTOS_DB_PASSWORD="a-mesma-senha-do-env"
.\mvnw.cmd spring-boot:run
```

O servico responde em `http://localhost:8081` por padrao.

## Executar com Compose

Na raiz do projeto:

```powershell
docker compose --profile prod up --build -d
docker compose --profile prod ps
```

No ambiente completo, o servico usa:

- Config Server: `http://config-server:8888`;
- PostgreSQL: `postgres-atendimentos:5432`;
- volume: `postgres-atendimentos-data`;
- redes: `atendimentos-network` e `services-network`.

## Testes

```powershell
.\mvnw.cmd test
```

O teste de integracao cria um PostgreSQL 17 temporario, carrega o contexto e persiste um atendimento. Sem Docker, o teste e marcado como ignorado por `disabledWithoutDocker`.

## Swagger

```text
http://localhost:8081/swagger-ui.html
http://localhost:8081/v3/api-docs
```

## Endpoints

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

Exemplo de criacao:

```json
{
  "dataHoraAtendimento": "2026-08-30T20:30:00",
  "tipoAtendimento": "URGENCIA",
  "statusAtendimento": "ANDAMENTO",
  "pacienteId": 1,
  "medicoId": 1
}
```

Valores aceitos para tipo:

```text
URGENCIA
INTERNACAO
AMBULATORIAL
```

Valores aceitos para status:

```text
ANDAMENTO
CANCELADO
CONCLUIDO
```

## Persistencia

O banco de atendimentos e independente do banco principal. Em `prod`, `JPA_DDL_AUTO=update` e fornecido pelo Compose para a primeira execucao academica. Os dados sobrevivem a `docker compose down` porque o PostgreSQL utiliza volume nomeado; `down -v` remove o volume e apaga os dados.

## Tratamento de erros

Bean Validation protege as entradas, e o `GlobalExceptionHandler` padroniza respostas `400`, `404` e `409`. Entidades JPA nao sao expostas diretamente como contrato HTTP.

## Validacao pendente

Os artefatos Docker foram preparados sem Docker disponivel no computador corporativo. A construcao da imagem, o healthcheck, a integracao pelo Compose e a persistencia apos reinicializacao devem ser validados em ambiente com Docker antes da tag `etapa-3`.
