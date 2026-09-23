# Etapa 4 - Comunicacao Assincrona e Processamento em Lote

## Objetivo

Introduzir duas formas de processamento alem da comunicacao REST sincrona: mensageria para comunicacao assincrona e Spring Batch para processamento em lote.

## O que implementar

- Escolher uma operacao do dominio que possa ocorrer de forma assincrona.
- Implementar produtor de mensagens.
- Configurar uma fila em um broker, como RabbitMQ ou tecnologia definida pelo professor.
- Implementar consumidor de mensagens.
- Demonstrar o comportamento quando o consumidor esta indisponivel.
- Escolher uma funcionalidade adequada para processamento em lote.
- Implementar um Job Spring Batch com Job, Step, ItemReader, ItemProcessor e ItemWriter.
- Configurar processamento em chunks.

## O que documentar no README

- Qual operacao foi escolhida para comunicacao assincrona.
- Por que essa operacao nao precisa terminar durante a requisicao original.
- O que acontece com a mensagem quando o consumidor esta indisponivel.
- Qual funcionalidade foi escolhida para processamento em lote.
- Por que essa funcionalidade e adequada para Batch.
- Em quais situacoes da aplicacao usar REST, mensageria ou Batch.

## O que nao fazer antes da hora

- Nao usar mensageria para operacao que exige resposta imediata.
- Nao publicar entidades completas quando apenas um DTO/evento simples basta.
- Nao implementar Batch para processar um unico registro isolado.
- Nao misturar responsabilidade de produtor, fila e consumidor sem clareza.

## Checklist de validacao

- Uma operacao publica mensagem no broker.
- A fila recebe a mensagem.
- Um consumidor processa a mensagem.
- A mensagem permanece aguardando quando o consumidor esta indisponivel.
- O Job Batch possui reader, processor e writer.
- O processamento em chunks esta configurado.
- Ha dados de exemplo suficientes para demonstrar o lote.
- O README diferencia REST, mensageria e Batch usando exemplos do proprio projeto.

## Marco esperado

Ao concluir e revisar a Etapa 4, registrar a tag:

```text
etapa-4
```
