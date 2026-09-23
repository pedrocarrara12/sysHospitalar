# Etapa 2 - Separacao e Comunicacao entre Servicos

## Objetivo

Evoluir a aplicacao organizada na Etapa 1 por meio da extracao de uma unica responsabilidade para uma nova aplicacao Spring Boot. A aplicacao principal deve continuar concentrando a maior parte das funcionalidades e passar a consumir o servico independente pela rede.

O foco desta etapa e compreender a mudanca entre uma chamada interna de classes Java e uma chamada HTTP entre duas aplicacoes que podem executar e falhar separadamente.

## Responsabilidade escolhida

A responsabilidade escolhida e o gerenciamento de `Atendimentos`, candidato identificado na Etapa 1.

O novo `atendimentos-service` sera responsavel por:

- cadastrar atendimentos;
- consultar atendimentos;
- atualizar atendimentos;
- remover atendimentos;
- filtrar por status e tipo;
- ordenar atendimentos por data e hora;
- persistir os dados de atendimento em seu proprio banco.

Pacientes, medicos e enfermeiros continuam pertencendo a aplicacao principal. O contrato entre as aplicacoes deve transportar apenas os dados necessarios, incluindo `pacienteId` e `medicoId`, sem compartilhar entidades JPA.

## Arquitetura esperada

```text
Cliente HTTP
    |
    v
Aplicacao principal
    Controller
        |
        v
    Service
        |-- Repository de Pacientes/Medicos
        |
        v
    AtendimentoClient (OpenFeign)
        |
        | HTTP
        v
atendimentos-service
    Controller
        |
        v
    Service
        |
        v
    Repository
        |
        v
    Banco de atendimentos
```

A comunicacao externa deve permanecer na camada de service da aplicacao principal. O controller nao deve chamar o Feign Client diretamente.

## Bloco 1 - Criar o servico independente

- Criar uma nova aplicacao Spring Boot chamada `atendimentos-service`.
- Configurar nome, porta e banco proprios.
- Manter apenas os componentes relacionados a Atendimentos.
- Garantir que a aplicacao principal e o novo servico possam ser iniciados separadamente.
- Nao criar um repositorio Git interno para o novo projeto.

## Bloco 2 - Expor a API REST

- Disponibilizar a responsabilidade de Atendimentos por meio de uma API REST.
- Utilizar recursos, metodos HTTP, parametros, corpos e codigos de resposta coerentes.
- Manter DTOs especificos para entrada e saida.
- Nao utilizar entidades JPA como contrato entre aplicacoes.
- Documentar no OpenAPI/Swagger:
  - endpoints;
  - operacoes disponiveis;
  - estruturas recebidas;
  - estruturas retornadas;
  - principais respostas HTTP de sucesso e erro.

O servico deve possuir pelo menos uma operacao efetivamente utilizada pela aplicacao principal. Neste projeto, a integracao deve contemplar os endpoints de Atendimento que permanecerem expostos pela aplicacao principal.

## Bloco 3 - Consumir o servico com OpenFeign

- Adicionar Spring Cloud OpenFeign somente a aplicacao principal.
- Habilitar a descoberta dos clientes Feign.
- Criar um `AtendimentoClient` responsavel por descrever as chamadas HTTP ao `atendimentos-service`.
- Fazer o `AtendimentoService` da aplicacao principal utilizar o `AtendimentoClient`.
- Manter no service principal as validacoes que dependem dos dados locais de Pacientes e Medicos.
- Nao implementar comunicacao HTTP diretamente no controller.

O fluxo esperado e:

```text
AtendimentoController
    -> AtendimentoService
    -> AtendimentoClient
    -> HTTP
    -> AtendimentoController do atendimentos-service
```

## Bloco 4 - Configurar o endereco externo

O endereco do servico remoto nao pode ficar fixo no codigo Java. Deve ser definido em `application.properties`, `application.yml` ou mecanismo equivalente.

Exemplo para execucao local:

```properties
atendimentos-service.url=${ATENDIMENTOS_SERVICE_URL:http://localhost:8081}
```

O valor local pode ser substituido pela variavel de ambiente `ATENDIMENTOS_SERVICE_URL` sem recompilar a aplicacao.

## Bloco 5 - Preservar o contrato publico

- Manter os endpoints publicos de Atendimento da aplicacao principal, salvo mudanca funcional explicitamente aprovada.
- Evitar alteracao silenciosa nos campos de entrada e saida existentes.
- O contrato interno do `atendimentos-service` pode usar apenas `pacienteId` e `medicoId`.
- Se a resposta publica continuar oferecendo `pacienteNome` e `medicoNome`, a aplicacao principal deve enriquecer a resposta usando seus dados locais.

## Bloco 6 - Remover a persistencia duplicada

Somente depois que a comunicacao estiver funcionando e validada:

- remover da aplicacao principal o acesso local ao `AtendimentoRepository`;
- remover a entidade JPA de Atendimento da aplicacao principal, caso nao tenha mais uso;
- remover mapeamentos internos que dependam da entidade local;
- confirmar que somente o `atendimentos-service` persiste atendimentos.

Nao manter dois bancos como fontes concorrentes para os mesmos atendimentos.

## Bloco 7 - Tratar falhas de comunicacao

- Testar chamadas com o `atendimentos-service` disponivel e indisponivel.
- Converter falhas de conexao em uma resposta HTTP controlada da aplicacao principal.
- Nao devolver ao cliente stack trace, endereco interno, classe do Feign ou detalhes tecnicos da conexao.
- Preservar respostas funcionais relevantes, como recurso nao encontrado, quando for possivel diferencia-las de indisponibilidade.
- Nao e obrigatorio adicionar Circuit Breaker, retries avancados ou outra biblioteca de resiliencia nesta etapa.

## Testes da comunicacao

Demonstrar com Postman, Swagger UI ou ferramenta equivalente:

1. funcionamento isolado da API do `atendimentos-service`;
2. funcionamento de Atendimento pela aplicacao principal, comprovando a chamada entre as aplicacoes;
3. resposta controlada da aplicacao principal quando o `atendimentos-service` estiver desligado.

Tambem validar:

- cadastro com paciente e medico existentes;
- rejeicao de paciente ou medico inexistente;
- consulta de atendimento existente e inexistente;
- filtros por status e tipo;
- atualizacao e remocao;
- preservacao dos endpoints e dos campos publicos existentes;
- execucao de `mvnw.cmd test` nas duas aplicacoes.

## O que documentar no README

Registrar:

- nome do servico independente;
- responsabilidade principal;
- funcionalidade removida ou separada da aplicacao principal;
- motivo da escolha e sua relacao com o dominio;
- como a aplicacao principal se comunica com o servico;
- configuracao necessaria para executar as duas aplicacoes;
- comportamento quando o servico fica indisponivel;
- evidencias ou roteiro dos testes de comunicacao.

Responder brevemente:

1. Qual funcionalidade foi separada da aplicacao principal?
2. Por que ela foi escolhida?
3. O que ficou mais complexo depois da separacao?
4. O que acontece com a funcionalidade principal quando o novo servico fica indisponivel?
5. A funcionalidade realmente precisa permanecer independente ou poderia continuar na aplicacao principal?

## O que nao fazer antes da hora

- Nao transformar todas as funcionalidades em microsservicos.
- Nao compartilhar entidades JPA entre as aplicacoes.
- Nao colocar a URL do servico diretamente no codigo Java.
- Nao chamar o Feign Client diretamente pelo controller.
- Nao ignorar falhas de rede.
- Nao adicionar Docker, Docker Compose, Config Server, mensageria ou Batch na Etapa 2.
- Nao criar mecanismos avancados de resiliencia sem necessidade para a atividade.

## Checklist de validacao

- [ ] A aplicacao principal e o `atendimentos-service` executam separadamente.
- [ ] O `atendimentos-service` possui responsabilidade clara e persistencia propria.
- [ ] A API utiliza DTOs e nao expoe entidades JPA.
- [ ] O Swagger documenta operacoes, contratos e respostas principais.
- [ ] A aplicacao principal possui um Feign Client dedicado.
- [ ] A comunicacao segue `Controller -> Service -> Feign Client`.
- [ ] A URL remota esta externalizada.
- [ ] Pelo menos uma operacao do servico e usada pela aplicacao principal.
- [ ] O contrato publico existente foi preservado ou a alteracao foi documentada.
- [ ] Somente o servico independente persiste atendimentos.
- [ ] A indisponibilidade do servico produz resposta controlada.
- [ ] Os tres cenarios de comunicacao foram demonstrados.
- [ ] Os testes das duas aplicacoes passam.
- [ ] O README contem a justificativa e a reflexao arquitetural completa.

## Marco esperado

Depois da implementacao, dos testes e da revisao final, registrar no repositorio a tag:

```text
etapa-2
```

A tag deve representar o primeiro momento em que as duas aplicacoes independentes se comunicam pela rede. Ela nao deve ser criada antes da validacao final.
