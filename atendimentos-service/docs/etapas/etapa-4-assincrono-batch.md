# Etapa 4 - Comunicação Assíncrona e Processamento em Lote

Este arquivo acompanha a documentação da aplicação principal.

A especificação revisada, os requisitos técnicos e o checklist completos da Etapa 4 estão em:

```text
../../../docs/etapas/etapa-4-assincrono-batch.md
```

Na implementação escolhida, o `atendimentos-service` continua responsável apenas por persistir atendimentos via REST. A aplicação principal publica o evento somente depois da confirmação dessa persistência; este serviço não recebe dependências de RabbitMQ, e-mail ou Spring Batch.

Este documento resumido evita manter duas especificações independentes e divergentes para a mesma etapa.
