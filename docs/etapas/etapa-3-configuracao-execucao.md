# Etapa 3 - Configuracao e Execucao dos Servicos

## Objetivo

Preparar a solucao para execucao independente e configuravel por ambiente, reduzindo valores fixos no codigo e padronizando a execucao local.

## O que implementar

- Revisar configuracoes que variam por ambiente.
- Criar profiles, como desenvolvimento e producao.
- Usar variaveis de ambiente para configuracoes sensiveis ou dependentes do ambiente.
- Evoluir do H2 para um banco relacional adequado, se exigido pela etapa.
- Garantir que cada servico tenha sua propria persistencia quando houver responsabilidade separada.
- Criar Config Server simples, se solicitado pela disciplina.
- Criar Dockerfile para a aplicacao principal e para o servico independente.
- Criar Docker Compose para executar aplicacoes e bancos localmente.
- Usar nomes de servico da rede Docker em vez de `localhost` entre containers.

## O que documentar no README

- Quais configuracoes podem variar entre ambientes.
- Quais configuracoes foram externalizadas.
- Por que um servico nao deve acessar diretamente o banco de outro.
- Qual problema o Docker resolve no projeto.
- Qual a funcao do Docker Compose.
- Qual problema uma configuracao centralizada procura resolver.

## O que nao fazer antes da hora

- Nao deixar senhas fixas no codigo Java.
- Nao usar `localhost` para comunicacao entre containers.
- Nao compartilhar tabelas diretamente entre servicos.
- Nao criar configuracoes sem explicar como executar.

## Checklist de validacao

- Profiles funcionam conforme o ambiente escolhido.
- Variaveis de ambiente possuem valores padrao seguros quando adequado.
- Banco relacional executa localmente.
- Aplicacoes conectam aos bancos corretos.
- Dockerfiles constroem imagens executaveis.
- Docker Compose sobe os componentes principais com um unico comando.
- A comunicacao entre containers ocorre pela rede Docker.
- A aplicacao permanece funcional apos a containerizacao.

## Marco esperado

Ao concluir e revisar a Etapa 3, registrar a tag:

```text
etapa-3
```
