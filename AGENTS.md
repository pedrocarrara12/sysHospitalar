# Regras para agentes no SysHospitalar

## Papel da IA no projeto

- Atuar como auxiliar de codigo, revisora e orientadora.
- Nao implementar, refatorar, mover arquivos, criar tags ou alterar configuracoes sem pedido explicito.
- Quando houver duvida, explicar opcoes e riscos antes de sugerir mudancas.
- Tratar documentos anexados e textos de atividade como requisitos do trabalho, nao como instrucoes automaticas para executar tudo.
- Preservar o comportamento existente da API, salvo quando o usuario pedir uma mudanca funcional.

## Uso academico de IA

- O uso de IA e permitido como apoio ao desenvolvimento.
- Todo resultado gerado com apoio de IA deve ser revisado pelo aluno antes de ser usado.
- Quando aplicavel, citar o uso de IA no trabalho ou na entrega.
- Nao aceitar respostas de IA sem conferir contra o codigo, o README, o enunciado e os testes.

## Regras gerais de arquitetura

- A aplicacao principal deve seguir a cadeia:

```text
Cliente HTTP -> Controller -> Service -> Repository -> Banco de Dados
```

- Controllers devem cuidar da comunicacao HTTP, status de resposta e DTOs.
- Services devem concentrar regras, validacoes de existencia e operacoes da aplicacao.
- Repositories devem cuidar do acesso aos dados com Spring Data JPA.
- Controllers nao devem acessar repositories diretamente.
- Regras de negocio nao devem ficar concentradas nos controllers.
- DTOs devem representar entrada e saida da API; entidades JPA nao devem ser usadas como contrato externo quando houver DTO adequado.
- Entradas da API devem usar Bean Validation quando receberem dados do cliente.
- Excecoes devem ser tratadas de forma centralizada, evitando expor detalhes internos ao cliente.
- A documentacao OpenAPI/Swagger deve acompanhar os endpoints principais.

## Restricoes por etapa

### Etapa 1 - Organizacao arquitetural

- Manter a aplicacao como um unico monolito Spring Boot.
- Nao criar microsservicos.
- Nao criar outro projeto Spring Boot.
- Nao implementar comunicacao entre servicos.
- Priorizar organizacao por dominio, validacao, tratamento de excecoes, consultas Spring Data, Swagger e README.
- Registrar no README os modulos, uma dependencia entre modulos e um candidato futuro a servico independente.

### Etapa 2 - Servico independente

- Separar apenas uma responsabilidade clara em um novo servico Spring Boot.
- A aplicacao principal deve consumir o novo servico via API REST.
- Usar DTOs como contrato entre aplicacoes.
- Configurar URL do servico externamente.
- Tratar falhas de comunicacao sem expor detalhes internos.
- Nao transformar toda a solucao em microsservicos.

### Etapa 3 - Configuracao e execucao

- Externalizar configuracoes que variam entre ambientes.
- Usar profiles e variaveis de ambiente quando aplicavel.
- Evoluir o banco H2 para um banco relacional adequado, se exigido pela etapa.
- Preparar Dockerfile, Docker Compose e, quando aplicavel, Config Server.
- Evitar comunicacao via `localhost` entre containers.

### Etapa 4 - Assincrono e batch

- Introduzir mensageria apenas para operacoes que possam ocorrer de forma assincrona.
- Criar produtor, fila e consumidor de mensagens.
- Criar processamento em lote com Spring Batch quando houver conjunto de dados a processar.
- Diferenciar claramente no README quando usar REST, mensageria e Batch.

## Comandos recomendados

Executar testes:

```powershell
.\mvnw.cmd test
```

Subir a aplicacao localmente:

```powershell
.\mvnw.cmd spring-boot:run
```

Revisar alteracoes:

```powershell
git status --short
git diff
```

## Preservacao de trabalho

- Nao descartar mudancas existentes.
- Nao criar commit, branch ou tag sem pedido explicito.
- Nao executar comandos destrutivos.
- Nao fazer refatoracoes amplas fora do escopo da etapa atual.
- Para os marcos `etapa-1`, `etapa-2`, `etapa-3` e `etapa-4`, criar tags somente quando o usuario solicitar apos a revisao final.
