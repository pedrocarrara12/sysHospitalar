# Etapa 3 - Configuração e Execução dos Serviços

## Competência avaliada

Desenvolver aplicações Cloud Native utilizando configuração externa, persistência independente e execução padronizada dos serviços.

## Objetivo

Evoluir a aplicação principal e o `atendimentos-service` criados nas etapas anteriores para que possam ser configurados e executados de forma independente e reproduzível, sem depender de valores fixos no código Java.

Ao final desta etapa, a solução deverá utilizar profiles, variáveis de ambiente, bancos relacionais persistentes, Spring Cloud Config Server, Dockerfiles e Docker Compose, preservando os endpoints e o comportamento funcional já existentes.

## Estado atual do projeto

Antes da implementação da Etapa 3, o projeto possui:

- uma aplicação principal na porta padrão `8080`;
- um `atendimentos-service` na porta `8081`;
- comunicação HTTP com OpenFeign;
- URL do serviço de atendimentos externalizada por `ATENDIMENTOS_SERVICE_URL`;
- um banco H2 em memória para a aplicação principal;
- outro banco H2 em memória para o `atendimentos-service`;
- ausência de profiles `dev` e `prod`;
- ausência de Config Server, Dockerfiles e Docker Compose.

Portanto, a Etapa 3 ainda não está implementada. Este documento define o trabalho necessário e os critérios para considerar a etapa concluída.

## Arquitetura esperada

```text
Cliente HTTP
    |
    v
Aplicação principal -----------------> Banco PostgreSQL principal
    |
    | HTTP usando o nome do serviço na rede Docker
    v
atendimentos-service ----------------> Banco PostgreSQL de atendimentos

Config Server
    |-------------------------------> Aplicação principal
    |-------------------------------> atendimentos-service

Todos os componentes são iniciados e conectados pelo Docker Compose.
```

Cada aplicação deverá acessar somente o banco pelo qual é responsável. A aplicação principal não poderá consultar tabelas do banco de atendimentos, e o `atendimentos-service` não poderá consultar tabelas de pacientes, médicos ou enfermeiros.

## Bloco 1 - Revisar e externalizar configurações

Identificar e retirar de valores fixos as configurações que variam conforme o ambiente:

- porta de cada aplicação;
- URL do Config Server;
- URL do `atendimentos-service`;
- URL JDBC de cada banco;
- usuário e senha de cada banco;
- estratégia de criação ou atualização do schema;
- exibição de SQL e outras opções específicas de execução.

Nenhuma dessas informações deverá ficar inserida diretamente no código Java.

Variáveis de ambiente previstas:

| Variável | Responsabilidade |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Selecionar o ambiente, como `dev` ou `prod` |
| `CONFIG_SERVER_URL` | Informar o endereço do Config Server |
| `ATENDIMENTOS_SERVICE_URL` | Informar o endereço HTTP do serviço de atendimentos |
| `DB_URL` | Informar a URL JDBC do banco da aplicação que recebe a variável |
| `DB_DRIVER` | Informar o driver JDBC enquanto a migração de banco não estiver concluída |
| `DB_USERNAME` | Informar o usuário do banco |
| `DB_PASSWORD` | Informar a senha do banco |
| `SERVER_PORT` | Permitir a alteração da porta da aplicação |
| `H2_CONSOLE_ENABLED` | Habilitar temporariamente o console H2 apenas em desenvolvimento |
| `JPA_DDL_AUTO` | Definir a estratégia de gerenciamento do schema pelo Hibernate |
| `JPA_SHOW_SQL` | Controlar a exibição das consultas SQL |
| `HIBERNATE_FORMAT_SQL` | Controlar a formatação do SQL exibido nos logs |

No Docker Compose, as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` deverão receber valores diferentes em cada container de aplicação. Senhas reais não deverão ser versionadas. Um arquivo `.env.example`, sem credenciais reais, poderá documentar os nomes esperados.

## Bloco 2 - Criar profiles de desenvolvimento e produção

Criar, no mínimo, os profiles:

- `dev`: execução local, com configurações convenientes para desenvolvimento;
- `prod`: execução por containers, com configurações externas e sem depender de `localhost` entre serviços.

As configurações comuns devem permanecer no arquivo base. Somente diferenças de ambiente devem ser colocadas nos arquivos de profile ou no repositório do Config Server.

Arquivos esperados, ou estrutura YAML equivalente:

```text
application.properties
application-dev.properties
application-prod.properties
```

Os dois profiles deverão ser exercitados sem alteração do código-fonte. Valores padrão são aceitáveis apenas quando forem seguros e úteis para desenvolvimento; credenciais de produção não devem possuir valor sensível fixo no repositório.

## Bloco 3 - Substituir o H2 por PostgreSQL

Como as duas aplicações ainda utilizam H2 em memória, a substituição por um banco relacional persistente faz parte desta etapa.

Implementar:

- driver PostgreSQL nas duas aplicações;
- datasource configurado por variáveis de ambiente;
- remoção das dependências e configurações exclusivas do H2 quando não forem mais necessárias;
- um banco para pacientes, médicos e enfermeiros;
- outro banco para atendimentos;
- volumes Docker distintos para preservar os dados entre reinicializações.

A separação pode ser feita com dois containers PostgreSQL, que deixa explícita a independência das persistências. Em qualquer solução adotada, cada aplicação deve possuir credenciais e base próprias e não pode acessar diretamente a estrutura da outra.

## Bloco 4 - Implementar o Spring Cloud Config Server

O Config Server é requisito do enunciado desta etapa e deverá atender às duas aplicações.

Criar uma terceira aplicação Spring Boot, dedicada somente à configuração centralizada, contendo:

- dependência do Spring Cloud Config Server;
- anotação de habilitação do servidor de configuração;
- porta configurável;
- repositório simples de configurações, que pode usar o modo `native` para a execução acadêmica local;
- configurações identificadas pelo `spring.application.name` de cada cliente e pelo profile ativo.

As aplicações clientes deverão importar suas configurações usando uma URL externa, por exemplo por meio de `CONFIG_SERVER_URL`. Configurações não sensíveis, como portas e URLs internas, podem ser centralizadas. Segredos devem continuar sendo fornecidos por variáveis de ambiente, e não gravados em texto puro no repositório de configuração.

Também deverá ser definido o comportamento quando o Config Server ainda não estiver pronto. A escolha entre falhar na inicialização ou usar importação opcional precisa ser coerente e documentada.

## Bloco 5 - Criar os Dockerfiles

Criar um Dockerfile para cada aplicação executável:

```text
Dockerfile                              # aplicação principal
atendimentos-service/Dockerfile         # serviço independente
config-server/Dockerfile                # servidor de configuração
```

Cada imagem deverá:

- gerar ou copiar o artefato da aplicação correspondente;
- usar uma imagem Java compatível com Java 21;
- expor a porta utilizada pela aplicação;
- iniciar somente o processo necessário para executar o serviço;
- permitir que as configurações sejam recebidas externamente.

Também devem ser utilizados arquivos `.dockerignore` quando necessários para evitar o envio de artefatos e diretórios desnecessários ao contexto de build.

## Bloco 6 - Orquestrar a solução com Docker Compose

Criar `compose.yml` ou `docker-compose.yml` na raiz para iniciar, com um único comando:

- Config Server;
- aplicação principal;
- `atendimentos-service`;
- PostgreSQL da aplicação principal;
- PostgreSQL do serviço de atendimentos.

O Compose deverá definir:

- builds ou imagens de todos os componentes;
- variáveis de ambiente necessárias;
- volumes nomeados para os bancos;
- rede de comunicação entre os serviços;
- portas publicadas apenas quando necessárias para acesso pelo host;
- healthchecks e dependências de inicialização quando úteis.

Dentro dos containers, não utilizar `localhost` para acessar outro componente. Devem ser usados os nomes dos serviços definidos no Compose, por exemplo:

```text
http://atendimentos-service:8081
http://config-server:8888
jdbc:postgresql://postgres-principal:5432/syshospitalar
jdbc:postgresql://postgres-atendimentos:5432/atendimentos
```

`localhost` continua válido apenas quando um processo no computador do usuário acessa uma porta publicada pelo container.

## Bloco 7 - Atualizar a documentação

Depois da implementação, atualizar o `README.md` principal e, quando aplicável, o README do `atendimentos-service` com:

- pré-requisitos para execução;
- profiles disponíveis e como selecioná-los;
- tabela de variáveis de ambiente;
- comando para construir e iniciar a solução;
- comando para encerrar a solução sem apagar os volumes;
- URLs de acesso à aplicação e ao Swagger;
- descrição dos bancos e dos volumes;
- roteiro de verificação da comunicação e da persistência;
- indicação clara de que o estado anterior usava H2 e o estado da Etapa 3 usa PostgreSQL.

Responder no README, relacionando as respostas à solução construída:

1. Quais configurações da aplicação podem variar entre ambientes?
2. Quais dessas configurações foram externalizadas?
3. Por que um serviço não deve acessar diretamente o banco de outro serviço?
4. Qual problema o Docker resolve no projeto?
5. Qual é a função do Docker Compose?
6. Qual problema uma configuração centralizada procura resolver?

## Roteiro de verificação

1. Executar os testes automatizados da aplicação principal.
2. Executar os testes automatizados do `atendimentos-service`.
3. Construir as imagens sem erros.
4. Iniciar toda a solução com Docker Compose.
5. Confirmar que as duas aplicações obtêm configuração do Config Server.
6. Confirmar que cada aplicação se conecta ao seu próprio PostgreSQL.
7. Criar paciente e médico pela aplicação principal.
8. Criar e consultar um atendimento pela aplicação principal, comprovando a chamada HTTP ao `atendimentos-service`.
9. Reiniciar os containers e confirmar que os dados permanecem nos volumes.
10. Confirmar que nenhuma comunicação entre containers usa `localhost`.
11. Exercitar os profiles `dev` e `prod` sem modificar o código-fonte.
12. Revisar logs e respostas para garantir que senhas e detalhes internos não sejam expostos.

## Checklist de conclusão

- [ ] Configurações variáveis não estão fixas no código Java.
- [ ] Existem configurações coerentes para os profiles `dev` e `prod`.
- [ ] Variáveis de ambiente são usadas para URLs, credenciais e demais valores externos.
- [ ] H2 foi substituído por PostgreSQL nas duas aplicações.
- [ ] Cada aplicação acessa somente seu próprio banco.
- [ ] O Config Server atende à aplicação principal e ao `atendimentos-service`.
- [ ] Segredos não estão versionados no Config Server nem nos arquivos da aplicação.
- [ ] Há um Dockerfile funcional para cada uma das três aplicações.
- [ ] O Docker Compose inicia aplicações, Config Server e bancos com um único comando.
- [ ] Os bancos utilizam volumes persistentes distintos.
- [ ] A comunicação entre containers usa nomes de serviço, e não `localhost`.
- [ ] A aplicação principal continua se comunicando com o serviço independente.
- [ ] Os dados permanecem disponíveis após reinicialização dos containers.
- [ ] Os testes das duas aplicações passam.
- [ ] O README contém instruções atualizadas e a reflexão arquitetural solicitada.

## Fora do escopo

- criar novos microsserviços além do `atendimentos-service` e do Config Server;
- introduzir mensageria ou Spring Batch, que pertencem à Etapa 4;
- alterar os contratos públicos da API sem necessidade;
- compartilhar tabelas, entidades JPA ou acesso direto ao banco entre aplicações;
- criar a tag antes da implementação, dos testes e da revisão final.

## Marco esperado

Somente depois de concluir a implementação, executar o roteiro de verificação e revisar a documentação, registrar no repositório a tag:

```text
etapa-3
```

A tag deve representar a versão configurada externamente, persistida em bancos relacionais e executada de forma integrada por containers. Ela não deve ser criada durante a preparação deste documento.
