# Config Server

Servidor de configuracao centralizada do SysHospitalar, criado para a Etapa 3 com Spring Cloud Config Server.

## Funcionamento

O servidor usa o profile `native` e le o repositorio localizado em:

```text
src/main/resources/config-repository
```

As configuracoes sao separadas pelo `spring.application.name` e pelo profile solicitado:

```text
pedro-carrara-syshospitalar-dev.properties
pedro-carrara-syshospitalar-prod.properties
atendimentos-service-dev.properties
atendimentos-service-prod.properties
```

O repositorio centraliza portas, URLs, datasource e opcoes JPA, mas nao armazena senhas. Credenciais continuam sendo fornecidas por variaveis de ambiente nos processos clientes.

## Executar localmente

Na raiz do SysHospitalar:

```powershell
$env:SPRING_PROFILES_ACTIVE="native"
$env:SERVER_PORT="8888"
.\mvnw.cmd -f config-server\pom.xml spring-boot:run
```

Consultas de verificacao:

```powershell
Invoke-RestMethod http://localhost:8888/pedro-carrara-syshospitalar/dev
Invoke-RestMethod http://localhost:8888/atendimentos-service/dev
Invoke-RestMethod http://localhost:8888/pedro-carrara-syshospitalar/prod
Invoke-RestMethod http://localhost:8888/atendimentos-service/prod
```

## Executar no Compose

No profile Compose `prod`, o servidor:

- recebe `SPRING_PROFILES_ACTIVE=native`;
- responde internamente em `http://config-server:8888`;
- participa da `services-network`;
- possui healthcheck baseado no endpoint de configuracao da aplicacao principal.

```powershell
docker compose --profile prod up --build -d
docker compose logs config-server
```

## Falha de configuracao

As aplicacoes clientes importam o Config Server de forma obrigatoria. Em `prod`, `spring.cloud.config.fail-fast=true`; portanto, os clientes falham na inicializacao quando o servidor de configuracao nao esta disponivel. O Compose evita a corrida de inicializacao aguardando o healthcheck do Config Server.

## Testes

Na raiz do projeto:

```powershell
.\mvnw.cmd -f config-server\pom.xml test
```

O teste de contexto usa o repositorio `native` e nao depende de Docker.

## Validacao pendente

A imagem e o healthcheck ainda precisam ser exercitados em um computador com Docker antes da criacao da tag `etapa-3`.
