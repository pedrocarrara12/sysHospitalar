# Etapa 2 - Separacao e Comunicacao entre Servicos

## Objetivo

Separar uma responsabilidade clara da aplicacao principal em um servico Spring Boot independente, mantendo a maior parte do sistema na aplicacao principal.

## O que implementar

- Escolher a funcionalidade candidata identificada na Etapa 1.
- Criar uma nova aplicacao Spring Boot para essa responsabilidade.
- Expor uma API REST no novo servico.
- Usar DTOs para os dados trocados entre aplicacoes.
- Documentar a API do novo servico com OpenAPI/Swagger.
- Fazer a aplicacao principal consumir o novo servico usando OpenFeign.
- Configurar a URL do servico externo fora do codigo Java.
- Tratar falhas de comunicacao com uma resposta adequada.

## O que documentar no README

- Nome do servico independente.
- Responsabilidade principal do servico.
- Funcionalidade removida ou separada da aplicacao principal.
- Motivo da separacao.
- Como a aplicacao principal se comunica com o servico.
- O que acontece quando o servico externo fica indisponivel.
- Reflexao sobre se a funcionalidade realmente precisa permanecer separada.

## O que nao fazer antes da hora

- Nao transformar todas as funcionalidades em microsservicos.
- Nao usar entidades JPA como contrato entre aplicacoes.
- Nao colocar URL do servico externo fixa no codigo Java.
- Nao ignorar falhas de rede.
- Nao adicionar Docker, Config Server, mensageria ou Batch se ainda estiver apenas na Etapa 2.

## Checklist de validacao

- Aplicacao principal e servico independente executam separadamente.
- O novo servico possui pelo menos uma operacao REST usada pela aplicacao principal.
- A comunicacao passa pela camada de service da aplicacao principal, nao diretamente pelo controller.
- DTOs representam os contratos entre as aplicacoes.
- Swagger documenta a API do novo servico.
- A aplicacao principal trata servico disponivel e indisponivel.
- Testes manuais via Postman, Swagger UI ou ferramenta equivalente demonstram a comunicacao.

## Marco esperado

Ao concluir e revisar a Etapa 2, registrar a tag:

```text
etapa-2
```
