# Guia prático — ambientes `dev` e `prod` com Docker Compose

## Objetivo

Este guia ensina a preparar dois modos de execução para o SysHospitalar:

- `dev`: somente os dois bancos PostgreSQL são executados em containers. O Config Server, o `atendimentos-service` e a aplicação principal são executados localmente pelo Maven;
- `prod`: os bancos, o Config Server e as duas aplicações são construídos e executados pelo Docker Compose.

O nome `prod` representa uma simulação acadêmica de produção. Ele não transforma o computador local em um ambiente de produção real.

O trabalho deve ser realizado na ordem apresentada. Ao terminar cada etapa, execute o respectivo checkpoint antes de continuar.

> Este documento é um roteiro de implementação. Os exemplos devem ser revisados e comparados com o código, o enunciado, o README e os testes antes da entrega.

## 1. Modelo mental

### Conceito

Existem dois tipos diferentes de profile neste projeto:

| Tipo | Responsabilidade | Exemplos |
| --- | --- | --- |
| Profile do Docker Compose | Seleciona quais containers serão iniciados | `dev`, `prod` |
| Profile do Spring | Seleciona quais propriedades uma aplicação Spring carregará | `dev`, `prod`, `native` |

Eles podem ter nomes parecidos, mas são mecanismos independentes.

### Ambiente `dev`

```text
Computador do desenvolvedor
|
|-- Config Server (Maven, profile native, porta 8888)
|-- atendimentos-service (Maven, profile dev, porta 8081)
|-- aplicação principal (Maven, profile dev, porta 8080)
|
`-- Docker Compose
    |-- postgres-principal:5432
    `-- postgres-atendimentos:5433 no host -> 5432 no container
```

Nesse ambiente, as aplicações executadas no computador acessam os bancos pelas portas publicadas em `localhost`.

### Ambiente `prod`

```text
Docker Compose
|
|-- aplicação-principal:8080
|     `-- postgres-principal:5432
|
|-- atendimentos-service:8081
|     `-- postgres-atendimentos:5432
|
`-- config-server:8888
```

Nesse ambiente, os componentes usam os nomes dos serviços do Compose. Um container nunca deve usar `localhost` para encontrar outro container.

### Por que `localhost` muda de significado?

- Para uma aplicação iniciada pelo Maven no Windows, `localhost` representa o computador do desenvolvedor.
- Dentro de um container, `localhost` representa somente aquele próprio container.
- Para acessar outro container, deve-se usar o nome do serviço, como `config-server` ou `postgres-principal`.

### Checkpoint

- [ ] Sei diferenciar profile do Compose de profile do Spring.
- [ ] Entendi por que `localhost` é válido em `dev`, mas não entre containers em `prod`.
- [ ] Entendi que `prod` é uma simulação local da topologia completa.


## 3. Contrato das variáveis de ambiente

### Conceito

O arquivo `.env` e as variáveis entregues aos containers têm papéis diferentes:

1. O Docker Compose lê o `.env` para substituir expressões como `${VARIAVEL}` no `compose.yml`.
2. O bloco `environment` de cada serviço define quais variáveis serão entregues ao processo dentro do container.
3. Uma aplicação iniciada diretamente pelo Maven não lê o `.env` automaticamente. Nesse caso, as variáveis devem ser definidas no terminal ou na configuração de execução da IDE.

Consulte a documentação oficial sobre [interpolação de variáveis no Compose](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/).

### Valores que pertencem ao `.env`

O `.env` deve conter valores locais que podem variar:

- portas publicadas no computador;
- nome de cada banco;
- usuário de cada banco;
- senha local de cada banco.

O `.env` não deve determinar indiscriminadamente o profile Spring de todos os containers. Se `SPRING_PROFILES_ACTIVE=prod` fosse aplicado também ao Config Server, ele deixaria de usar o profile `native` esperado.

### Valores que pertencem ao `compose.yml`

O Compose deve declarar explicitamente a topologia:

- aplicação principal e `atendimentos-service`: `SPRING_PROFILES_ACTIVE=prod`;
- Config Server: `SPRING_PROFILES_ACTIVE=native`;
- URL do Config Server: `http://config-server:8888`;
- URL do serviço: `http://atendimentos-service:8081`;
- URLs JDBC usando `postgres-principal` e `postgres-atendimentos`.

### Modelo de `.env.example`

Quando esta etapa for implementada, o `.env.example` deverá ficar equivalente a:

```dotenv
# Portas publicadas somente no computador local
CONFIG_SERVER_PORT=8888
SYSHOSPITALAR_SERVER_PORT=8080
ATENDIMENTOS_SERVER_PORT=8081
SYSHOSPITALAR_DB_PORT=5432
ATENDIMENTOS_DB_PORT=5433

# Banco da aplicação principal
SYSHOSPITALAR_DB_NAME=syshospitalar
SYSHOSPITALAR_DB_USERNAME=syshospitalar
SYSHOSPITALAR_DB_PASSWORD=defina-uma-senha-local

# Banco do serviço de atendimentos
ATENDIMENTOS_DB_NAME=atendimentos
ATENDIMENTOS_DB_USERNAME=atendimentos
ATENDIMENTOS_DB_PASSWORD=defina-outra-senha-local
```

O arquivo real deve ser criado com:

```powershell
Copy-Item .env.example .env
```

Depois, substitua as senhas ilustrativas no `.env`. O arquivo `.env` não deve ser versionado.

### Validação

Depois que o `compose.yml` existir, valide a interpolação com:

```powershell
docker compose --profile dev config
docker compose --profile prod config
docker compose config --environment
```

Esses comandos não iniciam containers. Eles mostram a configuração resolvida e ajudam a encontrar variáveis ausentes.

### Erros comuns

- acreditar que o Spring iniciado pelo Maven lê `.env` automaticamente;
- usar a mesma porta `5432` no host para os dois bancos;
- enviar `SPRING_PROFILES_ACTIVE=prod` para o Config Server;
- gravar senhas pessoais ou reais no `.env.example`;
- confundir a porta publicada no host com a porta interna do container.

### Checkpoint

- [ ] `.env.example` contém somente valores ilustrativos.
- [ ] `.env` está ignorado pelo Git.
- [ ] Cada banco possui nome, usuário, senha e porta próprios.
- [ ] O profile Spring será definido individualmente por serviço.

## 4. Dockerfiles das aplicações

### Conceito

Cada aplicação executável precisa de sua própria imagem. Uma construção multi-stage usa:

1. uma imagem com Maven e JDK para compilar;
2. uma imagem menor, somente com JRE, para executar o JAR.

Isso evita levar Maven, código-fonte e caches de build para a imagem final.

### Arquivos que deverão ser criados

```text
Dockerfile
atendimentos-service/Dockerfile
config-server/Dockerfile
.dockerignore
atendimentos-service/.dockerignore
config-server/.dockerignore
```

### Modelo para a aplicação principal

Arquivo `Dockerfile` na raiz:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/target/*.jar app.jar

USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Modelo para o `atendimentos-service`

Use a mesma estrutura em `atendimentos-service/Dockerfile`, alterando somente:

```dockerfile
EXPOSE 8081
```

Como o contexto de build será `./atendimentos-service`, os caminhos `pom.xml` e `src` continuarão corretos.

### Modelo para o Config Server

Use a mesma estrutura em `config-server/Dockerfile`, alterando somente:

```dockerfile
EXPOSE 8888
```

### Modelo de `.dockerignore`

Cada contexto de build deve possuir um `.dockerignore`:

```dockerignore
target/
.git/
.idea/
.vscode/
.mvn/
mvnw
mvnw.cmd
*.iml
.env
.env.*
!.env.example
README.md
docs/
```

Como a imagem principal copia somente o `pom.xml` e o diretório `src`, o `.dockerignore` da raiz também pode excluir os outros projetos e materiais que possuem contextos próprios:

```dockerignore
atendimentos-service/
config-server/
postman/
```

Não replique essas três exclusões nos contextos dos próprios subprojetos.

### Validação individual

Execute a partir da raiz:

```powershell
docker build -t syshospitalar-principal:local .
docker build -t syshospitalar-atendimentos:local .\atendimentos-service
docker build -t syshospitalar-config-server:local .\config-server
```

Confira as imagens:

```powershell
docker image ls
```

### Resultado esperado

As três imagens devem ser construídas sem erro e devem conter somente o JAR executável na etapa final.

### Erros comuns

- executar o build no contexto errado;
- copiar um `pom.xml` que pertence a outro módulo;
- usar uma imagem Java incompatível com Java 21;
- tentar executar os testes durante o build antes de preparar a infraestrutura de teste;
- executar o processo como `root` sem necessidade.

### Checkpoint

- [ ] Há um Dockerfile para cada aplicação.
- [ ] As três imagens usam Java 21.
- [ ] As imagens usam build multi-stage.
- [ ] A etapa final executa com usuário não privilegiado.
- [ ] As três imagens são construídas separadamente sem erro.

## 5. `compose.yml` com profiles

### Conceito

Um único arquivo declarará os cinco serviços. A propriedade `profiles` decidirá quais serviços participam de cada modo de execução.

| Serviço | Profile `dev` | Profile `prod` |
| --- | ---: | ---: |
| `postgres-principal` | Sim | Sim |
| `postgres-atendimentos` | Sim | Sim |
| `config-server` | Não | Sim |
| `atendimentos-service` | Não | Sim |
| `aplicacao-principal` | Não | Sim |

Consulte a documentação oficial sobre [profiles no Docker Compose](https://docs.docker.com/compose/how-tos/profiles/).

### Isolamento por redes

Serão usadas três redes:

```text
principal-network
  aplicação principal
  postgres-principal

atendimentos-network
  atendimentos-service
  postgres-atendimentos

services-network
  aplicação principal
  atendimentos-service
  config-server
```

Assim, o banco de atendimentos não fica na rede da aplicação principal, e o banco principal não fica na rede do serviço de atendimentos.

### Modelo completo

Crie `compose.yml` na raiz:

```yaml
services:
  postgres-principal:
    image: postgres:17-alpine
    profiles: ["dev", "prod"]
    environment:
      POSTGRES_DB: ${SYSHOSPITALAR_DB_NAME}
      POSTGRES_USER: ${SYSHOSPITALAR_DB_USERNAME}
      POSTGRES_PASSWORD: ${SYSHOSPITALAR_DB_PASSWORD}
    ports:
      - "127.0.0.1:${SYSHOSPITALAR_DB_PORT}:5432"
    volumes:
      - postgres-principal-data:/var/lib/postgresql/data
    networks:
      - principal-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  postgres-atendimentos:
    image: postgres:17-alpine
    profiles: ["dev", "prod"]
    environment:
      POSTGRES_DB: ${ATENDIMENTOS_DB_NAME}
      POSTGRES_USER: ${ATENDIMENTOS_DB_USERNAME}
      POSTGRES_PASSWORD: ${ATENDIMENTOS_DB_PASSWORD}
    ports:
      - "127.0.0.1:${ATENDIMENTOS_DB_PORT}:5432"
    volumes:
      - postgres-atendimentos-data:/var/lib/postgresql/data
    networks:
      - atendimentos-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  config-server:
    build:
      context: ./config-server
    profiles: ["prod"]
    environment:
      SPRING_PROFILES_ACTIVE: native
      SERVER_PORT: 8888
    ports:
      - "127.0.0.1:${CONFIG_SERVER_PORT}:8888"
    networks:
      - services-network
    healthcheck:
      test:
        - CMD-SHELL
        - wget -q --spider http://localhost:8888/pedro-carrara-syshospitalar/prod || exit 1
      interval: 5s
      timeout: 5s
      retries: 12
      start_period: 15s

  atendimentos-service:
    build:
      context: ./atendimentos-service
    profiles: ["prod"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      CONFIG_SERVER_URL: http://config-server:8888
      SERVER_PORT: 8081
      DB_URL: jdbc:postgresql://postgres-atendimentos:5432/${ATENDIMENTOS_DB_NAME}
      DB_USERNAME: ${ATENDIMENTOS_DB_USERNAME}
      DB_PASSWORD: ${ATENDIMENTOS_DB_PASSWORD}
      JPA_DDL_AUTO: update
    ports:
      - "127.0.0.1:${ATENDIMENTOS_SERVER_PORT}:8081"
    depends_on:
      config-server:
        condition: service_healthy
      postgres-atendimentos:
        condition: service_healthy
    networks:
      - atendimentos-network
      - services-network
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8081/v3/api-docs || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 12
      start_period: 20s

  aplicacao-principal:
    build:
      context: .
    profiles: ["prod"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      CONFIG_SERVER_URL: http://config-server:8888
      ATENDIMENTOS_SERVICE_URL: http://atendimentos-service:8081
      SERVER_PORT: 8080
      DB_URL: jdbc:postgresql://postgres-principal:5432/${SYSHOSPITALAR_DB_NAME}
      DB_USERNAME: ${SYSHOSPITALAR_DB_USERNAME}
      DB_PASSWORD: ${SYSHOSPITALAR_DB_PASSWORD}
      JPA_DDL_AUTO: update
    ports:
      - "127.0.0.1:${SYSHOSPITALAR_SERVER_PORT}:8080"
    depends_on:
      config-server:
        condition: service_healthy
      postgres-principal:
        condition: service_healthy
      atendimentos-service:
        condition: service_healthy
    networks:
      - principal-network
      - services-network
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/v3/api-docs || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 12
      start_period: 20s

volumes:
  postgres-principal-data:
  postgres-atendimentos-data:

networks:
  principal-network:
  atendimentos-network:
  services-network:
```

### Por que não usar `container_name`?

O Compose já fornece descoberta por DNS usando o nome do serviço. Evitar `container_name` reduz conflitos entre projetos e permite que o Compose gerencie os nomes.

### Por que usar healthchecks?

`depends_on` sem condição garante ordem de criação, mas não garante que o banco ou o Config Server já estejam prontos. Com `service_healthy`, o cliente aguarda o healthcheck da dependência.

Consulte a documentação oficial sobre [ordem de inicialização no Compose](https://docs.docker.com/compose/how-tos/startup-order/).

### Limitação consciente do arquivo único

Como os mesmos serviços PostgreSQL participam de `dev` e `prod`, suas portas também ficam publicadas durante a simulação `prod`. O vínculo com `127.0.0.1` impede acesso por outras máquinas da rede local.

Em uma implantação real, normalmente seriam usados arquivos de override ou uma plataforma de orquestração, e os bancos não teriam portas publicadas para o host.

### Validação estática

Antes de iniciar containers:

```powershell
docker compose --profile dev config
docker compose --profile prod config
```

### Resultado esperado

- nenhuma variável obrigatória ausente;
- cinco serviços visíveis na configuração de `prod`;
- somente dois bancos selecionados pelo profile `dev`;
- nenhuma URL interna usando `localhost`.

### Erros comuns

- usar `$POSTGRES_USER` no healthcheck e fazer o Compose interpolar a variável antes da hora; dentro do YAML, use `$${POSTGRES_USER}`;
- colocar os dois bancos na mesma porta do host;
- usar `localhost` em `DB_URL` dentro das aplicações em container;
- não aguardar o Config Server ficar saudável;
- conectar as duas aplicações às duas redes de banco, anulando o isolamento.

### Checkpoint

- [ ] O Compose é válido nos profiles `dev` e `prod`.
- [ ] Existem dois volumes nomeados distintos.
- [ ] Existem três redes com responsabilidades claras.
- [ ] Bancos e Config Server possuem healthchecks.
- [ ] Cada aplicação recebe somente as credenciais de seu banco.

## 6. Executar o ambiente `dev`

### Etapa 1 — iniciar somente os bancos

Na raiz do projeto:

```powershell
docker compose --profile dev up -d
docker compose --profile dev ps
```

### Resultado esperado

Somente estes serviços devem aparecer:

```text
postgres-principal
postgres-atendimentos
```

Os dois devem alcançar o estado `healthy`.

### Etapa 2 — iniciar o Config Server localmente

Abra um PowerShell na raiz:

```powershell
$env:SPRING_PROFILES_ACTIVE="native"
$env:SERVER_PORT="8888"
.\mvnw.cmd -f config-server\pom.xml spring-boot:run
```

Valide em outro terminal:

```powershell
Invoke-RestMethod http://localhost:8888/pedro-carrara-syshospitalar/dev
Invoke-RestMethod http://localhost:8888/atendimentos-service/dev
```

### Etapa 3 — iniciar o `atendimentos-service`

Abra outro PowerShell na pasta `atendimentos-service`:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:CONFIG_SERVER_URL="http://localhost:8888"
$env:ATENDIMENTOS_DB_URL="jdbc:postgresql://localhost:5433/atendimentos"
$env:ATENDIMENTOS_DB_USERNAME="atendimentos"
$env:ATENDIMENTOS_DB_PASSWORD="a-mesma-senha-do-env"
.\mvnw.cmd spring-boot:run
```

Valide:

```text
http://localhost:8081/swagger-ui.html
http://localhost:8081/v3/api-docs
```

### Etapa 4 — iniciar a aplicação principal

Abra outro PowerShell na raiz:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:CONFIG_SERVER_URL="http://localhost:8888"
$env:ATENDIMENTOS_SERVICE_URL="http://localhost:8081"
$env:SYSHOSPITALAR_DB_URL="jdbc:postgresql://localhost:5432/syshospitalar"
$env:SYSHOSPITALAR_DB_USERNAME="syshospitalar"
$env:SYSHOSPITALAR_DB_PASSWORD="a-mesma-senha-do-env"
.\mvnw.cmd spring-boot:run
```

Valide:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### Etapa 5 — confirmar o profile carregado

Os logs das aplicações devem indicar o profile `dev`. Os logs do Config Server devem indicar `native`.

### Erros comuns

#### A senha do banco não funciona

O PostgreSQL inicializa usuário e senha somente na primeira criação do volume. Alterar o `.env` depois não altera automaticamente um banco já criado.

Antes de remover volumes, confirme que os dados podem ser descartados. A remoção de volumes é destrutiva.

#### A aplicação não encontra o Config Server

Confirme:

- o Config Server foi iniciado primeiro;
- a porta `8888` está livre;
- `CONFIG_SERVER_URL` usa `http://localhost:8888`;
- os endpoints de configuração respondem no navegador ou PowerShell.

#### A aplicação não conecta ao banco

Confirme que:

- o banco está `healthy`;
- a aplicação principal usa a porta `5432`;
- o serviço de atendimentos usa a porta `5433`;
- usuário e senha são os mesmos do `.env`.

### Checkpoint

- [ ] O Compose iniciou apenas os bancos.
- [ ] O Config Server responde aos dois clientes.
- [ ] O `atendimentos-service` iniciou com `dev`.
- [ ] A aplicação principal iniciou com `dev`.
- [ ] Os dois Swagger estão acessíveis.
- [ ] A comunicação da aplicação principal com o serviço funciona.

## 7. Executar o ambiente `prod`

### Conceito

No profile `prod` do Compose, toda a solução é construída e iniciada. Os profiles Spring são atribuídos individualmente pelo próprio `compose.yml`.

### Inicialização

Na raiz:

```powershell
docker compose --profile prod up --build -d
docker compose --profile prod ps
```

### Resultado esperado

Devem existir cinco serviços:

```text
postgres-principal
postgres-atendimentos
config-server
atendimentos-service
aplicacao-principal
```

Depois do período de inicialização, todos devem estar em execução e os serviços com healthcheck devem aparecer como `healthy`.

### Profiles Spring esperados

| Serviço | Profile Spring |
| --- | --- |
| `config-server` | `native` |
| `atendimentos-service` | `prod` |
| `aplicacao-principal` | `prod` |

### Endereços internos esperados

```text
Config Server:          http://config-server:8888
Serviço de atendimento: http://atendimentos-service:8081
Banco principal:        postgres-principal:5432
Banco de atendimentos:  postgres-atendimentos:5432
```

Esses nomes funcionam dentro das redes do Compose. Para acessar pelo Windows, continuam sendo usadas as portas publicadas em `localhost`.

### Validação pelo host

```powershell
Invoke-RestMethod http://localhost:8888/pedro-carrara-syshospitalar/prod
Invoke-RestMethod http://localhost:8888/atendimentos-service/prod
Invoke-WebRequest http://localhost:8080/v3/api-docs
Invoke-WebRequest http://localhost:8081/v3/api-docs
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8081/swagger-ui.html
```

### Conferir logs

```powershell
docker compose logs config-server
docker compose logs atendimentos-service
docker compose logs aplicacao-principal
```

Procure pelos profiles ativos e confirme que não aparecem tentativas de conexão com outros componentes por `localhost`.

### Checkpoint

- [ ] Os cinco serviços foram iniciados.
- [ ] Config Server usa `native`.
- [ ] As aplicações clientes usam `prod`.
- [ ] Os clientes obtêm configuração do Config Server.
- [ ] Cada aplicação conecta somente ao próprio PostgreSQL.
- [ ] Não há comunicação entre containers por `localhost`.

## 8. Verificação funcional e persistência

### Fluxo funcional mínimo

Usando Swagger ou a coleção Postman do projeto:

1. crie um paciente pela aplicação principal;
2. crie um médico pela aplicação principal;
3. crie um atendimento pela aplicação principal usando os identificadores anteriores;
4. consulte o atendimento novamente pela aplicação principal;
5. confirme nos logs que ocorreu uma chamada HTTP ao `atendimentos-service`.

### Testar persistência

Pare a solução sem remover os volumes:

```powershell
docker compose --profile prod down
```

Inicie novamente:

```powershell
docker compose --profile prod up -d
```

Repita as consultas. Paciente, médico e atendimento devem continuar disponíveis.

### Parar o ambiente `dev`

```powershell
docker compose --profile dev down
```

### Parar o ambiente `prod`

```powershell
docker compose --profile prod down
```

### Atenção aos volumes

O comando abaixo apaga os volumes e, portanto, os dados dos bancos:

```powershell
docker compose --profile prod down -v
```

Use `-v` somente quando houver intenção explícita de reinicializar os bancos do zero.

### Diagnóstico

#### Mostrar estado

```powershell
docker compose --profile prod ps
```

#### Acompanhar todos os logs

```powershell
docker compose --profile prod logs -f
```

#### Acompanhar um serviço

```powershell
docker compose logs -f config-server
```

#### Conferir a configuração resolvida

```powershell
docker compose --profile prod config
docker compose config --environment
```

#### Reiniciar apenas um serviço

```powershell
docker compose restart atendimentos-service
```

### Checkpoint

- [ ] O fluxo paciente, médico e atendimento funciona.
- [ ] A aplicação principal continua consumindo o serviço por HTTP.
- [ ] Os dados sobrevivem ao `down` seguido de `up`.
- [ ] Sei consultar estado e logs dos containers.
- [ ] Sei a diferença entre `down` e `down -v`.

## 9. Schema do banco nesta atividade

### Decisão acadêmica

Os arquivos de produção usam `ddl-auto=validate` como padrão seguro. Entretanto, bancos recém-criados não possuem tabelas para validar.

Para permitir a primeira execução desta atividade sem adicionar uma ferramenta de migração agora, o Compose fornece:

```yaml
JPA_DDL_AUTO: update
```

Isso é uma concessão para a simulação acadêmica local.

### Prática recomendada em produção real

Em projetos profissionais:

1. crie migrations versionadas com Flyway ou Liquibase;
2. aplique as migrations na inicialização ou no pipeline;
3. mantenha o Hibernate em `validate`;
4. não permita que o Hibernate altere automaticamente o schema de produção.

### Checkpoint

- [ ] Entendi por que esta atividade usa `update` temporariamente.
- [ ] Entendi por que produção real deve usar migrations e `validate`.

## 10. Quando usar profiles ou arquivos de override

### Um Compose com profiles

É adequado quando:

- a topologia é pequena;
- os ambientes compartilham quase todos os serviços;
- a principal diferença é selecionar quais serviços iniciam;
- o projeto é local, acadêmico ou de desenvolvimento.

### Compose base com overrides

É preferível quando:

- portas, volumes, recursos e políticas variam significativamente;
- produção não deve publicar portas de banco;
- existem configurações diferentes de observabilidade ou segurança;
- o mesmo serviço precisa de definições muito diferentes por ambiente.

Neste projeto foi escolhido um único arquivo com profiles para tornar o aprendizado e a operação mais simples.

## 11. Checklist reutilizável para outros projetos

### Configuração

- [ ] Configurações variáveis estão fora do código-fonte.
- [ ] Segredos reais não estão versionados.
- [ ] `.env.example` documenta somente valores ilustrativos.
- [ ] Cada aplicação recebe apenas as variáveis necessárias.
- [ ] Profiles Spring têm responsabilidades claras.

### Imagens

- [ ] Cada processo possui sua própria imagem.
- [ ] Build e runtime usam estágios separados.
- [ ] A versão do Java é compatível com o projeto.
- [ ] O processo final não executa como `root`.
- [ ] `.dockerignore` reduz o contexto de build.

### Compose

- [ ] Serviços usam nomes DNS do Compose.
- [ ] Não existe comunicação entre containers via `localhost`.
- [ ] Bancos possuem volumes persistentes diferentes.
- [ ] Bancos possuem credenciais diferentes.
- [ ] Redes limitam acessos desnecessários.
- [ ] Dependências críticas possuem healthchecks.
- [ ] Portas são publicadas somente quando necessárias.

### Validação

- [ ] A configuração foi validada com `docker compose config`.
- [ ] Imagens foram construídas individualmente.
- [ ] O ambiente mínimo inicia corretamente.
- [ ] O ambiente completo inicia corretamente.
- [ ] Comunicação entre serviços foi exercitada.
- [ ] Persistência foi verificada após reinicialização.
- [ ] Logs não expõem senhas nem detalhes internos.

## 12. Critérios de conclusão deste guia

A implementação orientada por este documento estará concluída quando:

- `docker compose --profile dev up -d` iniciar somente os dois PostgreSQL;
- `docker compose --profile prod up --build -d` iniciar os cinco componentes;
- a aplicação principal acessar somente o banco principal;
- o `atendimentos-service` acessar somente o banco de atendimentos;
- as aplicações clientes receberem configuração do Config Server;
- nenhuma comunicação entre containers usar `localhost`;
- o fluxo funcional completo continuar funcionando;
- os dados permanecerem disponíveis após reinicialização;
- os testes automatizados forem executados e revisados;
- o README for atualizado com os comandos finais realmente validados.

Somente depois da implementação, dos testes e da revisão final deverá ser considerada a criação da tag `etapa-3`, e apenas mediante solicitação explícita.

## Referências oficiais

- [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
- [Profiles no Docker Compose](https://docs.docker.com/compose/how-tos/profiles/)
- [Variáveis e interpolação no Compose](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)
- [Ordem de inicialização e healthchecks](https://docs.docker.com/compose/how-tos/startup-order/)
- [Redes no Docker Compose](https://docs.docker.com/compose/how-tos/networking/)
- [Spring Cloud Config Client](https://docs.spring.io/spring-cloud-config/reference/client.html)
