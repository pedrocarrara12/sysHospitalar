# Etapa 3 - Configuração e Execução dos Serviços

Este arquivo acompanha a documentação da aplicação principal.

A especificação revisada, o plano de implementação e o checklist completos da Etapa 3 estão em:

```text
../../../docs/etapas/etapa-3-configuracao-execucao.md
```

O `atendimentos-service` participa da Etapa 3 com:

- profiles `dev` e `prod`;
- configurações externas e variáveis de ambiente;
- banco PostgreSQL próprio, sem acesso ao banco da aplicação principal;
- consumo de configuração pelo Spring Cloud Config Server;
- Dockerfile próprio;
- execução integrada pelo Docker Compose da raiz;
- comunicação entre containers por nomes de serviço, sem usar `localhost`.

Este documento resumido evita manter duas especificações independentes e divergentes para a mesma etapa.
