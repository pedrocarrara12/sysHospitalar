# Etapa 4 - Comunicação Assíncrona e Processamento em Lote

Este documento consolida os requisitos técnicos e o roteiro de validação da Etapa 4. Ele não define, por si só, que todas as sugestões abaixo já foram implementadas.

O planejamento técnico da operação assíncrona escolhida está em [Notificação assíncrona por e-mail](./etapa-4-planejamento-notificacao-email.md).

O planejamento técnico do lote está em [Importação de pacientes com Spring Batch](./etapa-4-planejamento-batch-pacientes.md).

Para compreender a solução implementada, consulte o [Guia didático da implementação](./etapa-4-guia-implementacao.md).

Para executar as verificações automáticas e manuais, consulte o [Guia completo de testes](./etapa-4-guia-testes.md).

## Competência e objetivo

Evoluir o SysHospitalar com duas formas de processamento diferentes da comunicação síncrona por API REST:

- comunicação assíncrona por mensageria;
- processamento estruturado de um conjunto de dados com Spring Batch.

Ao final da etapa, a solução e o README devem deixar claro quando usar REST, mensageria ou Batch. A proposta não exige uma arquitetura complexa orientada a eventos nem a transformação de toda a solução em microsserviços.

## Escopo obrigatório

### 1. Operação assíncrona do domínio

Escolher uma operação coerente com o domínio hospitalar que não precise terminar durante a requisição original. Exemplos possíveis são o envio ou a simulação de uma notificação após um atendimento, o registro de histórico ou o processamento posterior de uma solicitação.

A escolha deve preservar como síncronas as operações que precisam de resposta imediata ao cliente.

### 2. Mensageria

Usar o broker definido para a disciplina, como RabbitMQ ou tecnologia equivalente, e implementar o fluxo completo:

```text
Aplicação produtora -> mensagem -> fila no broker -> consumidor
```

São obrigatórios:

- um produtor que publique a mensagem a partir da operação escolhida;
- uma fila configurada no broker;
- um consumidor que receba e processe a mensagem;
- configuração externa dos dados de conexão que variem entre ambientes;
- integração do broker à forma de execução adotada pelo projeto, quando aplicável.

A mensagem deve usar um DTO/evento próprio e conter somente as informações necessárias para o consumidor cumprir sua responsabilidade. Não é necessário criar contrato complexo, versionamento avançado de eventos ou uma arquitetura ampla orientada a eventos.

Exemplo meramente ilustrativo para o domínio:

```json
{
  "tipo": "ATENDIMENTO_REGISTRADO",
  "atendimentoId": 10,
  "destinatario": "paciente@email.com"
}
```

O consumidor pode registrar a informação, atualizar dado sob sua responsabilidade, gerar uma notificação ou simular o envio de uma comunicação. O resultado deve ser observável, por exemplo, por log ou persistência, sem expor detalhes internos ao cliente HTTP.

### 3. Demonstração da comunicação assíncrona

Preparar um cenário ponta a ponta no qual seja possível observar:

1. uma operação sendo realizada na aplicação;
2. o produtor publicando a mensagem;
3. a mensagem chegando e permanecendo na fila;
4. o consumidor recebendo e processando a mensagem.

Também é obrigatório demonstrar a indisponibilidade temporária do consumidor:

1. manter o broker disponível e interromper somente o consumidor;
2. executar a operação que publica a mensagem;
3. comprovar que a mensagem permanece aguardando no broker;
4. reiniciar o consumidor;
5. comprovar que a mensagem é então processada.

Esse cenário evidencia a diferença em relação ao REST: o produtor não precisa aguardar a conclusão do processamento feito pelo consumidor.

### 4. Processamento em lote com Spring Batch

Escolher uma funcionalidade relacionada ao domínio que processe múltiplos registros. Uma importação de pacientes por CSV é uma possibilidade, mas a decisão final pode usar outro conjunto coerente, como atualização de cadastros ou de status.

O Job deve possuir explicitamente:

- `Job`;
- pelo menos um `Step`;
- `ItemReader` para ler a fonte de dados;
- `ItemProcessor` para aplicar uma regra do domínio;
- `ItemWriter` para gravar ou registrar o resultado;
- processamento orientado a chunks, com tamanho configurado de forma explícita.

### 5. Fonte, processamento e destino do lote

A fonte deve conter múltiplos registros relacionados ao domínio. Um arquivo CSV é suficiente para a atividade.

```text
Fonte com múltiplos registros
        |
        v
   ItemReader
        |
        v
 ItemProcessor
        |
        v
   ItemWriter
        |
        v
      Destino
```

O `ItemProcessor` deve executar ao menos uma regra observável, como validação, normalização de texto, conversão de formato, cálculo, complementação de dados ou descarte de registro inválido.

O `ItemWriter` pode usar banco de dados, arquivo ou outro mecanismo aceito na disciplina. Se o destino for um banco, a escrita deve usar a persistência pertencente à aplicação responsável pelo Job; um serviço não deve acessar diretamente o banco de outro serviço.

Não é necessário testar grandes volumes. O objetivo é demonstrar corretamente o ciclo leitura, processamento e escrita em chunks.

## Arquitetura esperada

A Etapa 4 complementa, sem substituir, o que foi construído nas etapas anteriores:

```text
Cliente HTTP -> API REST -> aplicação principal
                            |
                            +-> HTTP -> atendimentos-service
                            |
                            `-> mensagem -> broker -> consumidor

Fonte de dados -> Spring Batch -> processamento -> destino
```

Nem todos os componentes precisam estar diretamente ligados. Cada recurso deve existir porque atende a uma necessidade específica da solução.

## Documentação obrigatória no README

O README final deve registrar, com respostas relacionadas ao SysHospitalar:

- qual operação foi escolhida para a comunicação assíncrona;
- por que ela não precisa terminar durante a requisição original;
- qual mensagem é publicada e quais dados mínimos ela transporta;
- o que o consumidor faz ao receber a mensagem;
- o que acontece quando o consumidor fica temporariamente indisponível;
- como executar e observar o cenário completo de mensageria;
- qual funcionalidade foi escolhida para o processamento em lote;
- qual é a fonte dos dados, a regra do processor e o destino do writer;
- por que a funcionalidade escolhida é adequada para Batch;
- como executar e verificar o Job;
- em quais situações concretas do projeto é mais adequado usar REST, mensageria ou Batch.

Uma distinção esperada é:

| Mecanismo | Quando usar no projeto |
|---|---|
| REST | Quando o cliente ou outro serviço precisa de resposta imediata. |
| Mensageria | Quando o trabalho pode ocorrer depois e o produtor não deve aguardar o consumidor. |
| Batch | Quando vários registros precisam passar por um fluxo estruturado de leitura, processamento e escrita. |

## Limites da etapa

- Não usar mensageria em operação que exige resposta imediata.
- Não publicar entidades JPA completas quando um DTO/evento simples for suficiente.
- Não criar versionamento avançado de eventos sem necessidade acadêmica ou funcional.
- Não usar Spring Batch para um único registro isolado.
- Não criar novos microsserviços apenas para aparentar uma arquitetura mais distribuída.
- Não compartilhar bancos entre aplicações.
- Não substituir por mensageria a comunicação REST da Etapa 2 quando ela continuar sendo a opção adequada.

## Checklist de aceite

### Mensageria

- [x] A operação assíncrona escolhida é coerente com o domínio e está explicada.
- [x] Existe produtor de mensagens.
- [x] Existe fila configurada em um broker.
- [x] Existe consumidor de mensagens.
- [x] A mensagem contém somente os dados necessários.
- [x] A publicação não exige que o consumidor conclua o trabalho durante a requisição original.
- [x] É possível observar publicação, permanência na fila e consumo em teste de integração.
- [x] Com o consumidor parado, a mensagem permanece no broker e é processada após sua volta em teste de integração.

### Spring Batch

- [x] A funcionalidade processa um conjunto de informações do domínio.
- [x] A fonte contém múltiplos registros de exemplo.
- [x] Há `Job`, `Step`, `ItemReader`, `ItemProcessor` e `ItemWriter`.
- [x] O processor aplica regras de negócio e transformações observáveis.
- [x] O writer grava o resultado em destino adequado.
- [x] O processamento em chunks de 10 está configurado.
- [x] O banco pertence à aplicação principal, responsável pelo Job.
- [x] A execução e o resultado do lote podem ser demonstrados pelos endpoints de início e consulta.

### Documentação e validação

- [x] O README responde às perguntas de reflexão arquitetural.
- [x] O README diferencia REST, mensageria e Batch com exemplos do SysHospitalar.
- [x] O README ensina a iniciar o broker, executar o cenário assíncrono e executar o Job.
- [x] A arquitetura documentada corresponde ao código e à configuração reais.
- [x] Os testes existentes continuam passando nas três aplicações.
- [ ] A demonstração manual da mensageria e do Batch foi revisada pelo aluno.

## Situação da implementação

Em 4 de outubro de 2026, foram implementados o fluxo RabbitMQ com consumidor de e-mail, retentativas e DLQ, e a importação de pacientes com Spring Batch. Os itens marcados acima possuem evidência no código ou em testes automatizados. A demonstração manual e o envio por Gmail permanecem sob responsabilidade do aluno e não são marcados como verificados automaticamente.

## Marco da etapa

Somente após a implementação, os testes, a demonstração e a revisão final, registrar a tag:

```text
etapa-4
```

A tag deve representar exatamente a versão final submetida para avaliação e só deve ser criada quando solicitada pelo usuário.
