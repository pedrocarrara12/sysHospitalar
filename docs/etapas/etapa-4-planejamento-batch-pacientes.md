# Etapa 4 - Planejamento da importação de pacientes com Spring Batch

## Objetivo

Importar vários pacientes de um arquivo CSV UTF-8 por meio de um Job Spring Batch executado de forma assíncrona. A API aceita o arquivo, inicia a execução e devolve `202 Accepted`; o cliente acompanha o resultado por um segundo endpoint.

Essa funcionalidade é apropriada para Batch porque possui uma fonte com múltiplos registros, regras repetidas de validação e normalização e escrita transacional em blocos. O cadastro individual de paciente continua disponível pela API REST.

## Fluxo

```text
POST multipart
    -> validação básica e arquivo temporário
    -> JobOperator
    -> FlatFileItemReader
    -> ItemProcessor
    -> JpaItemWriter (chunks de 10)
    -> PostgreSQL principal

GET /batch/pacientes/importacoes/{executionId}
    -> metadados do Spring Batch
    -> estado e contadores sem dados pessoais
```

## Contrato do CSV

O arquivo deve ter extensão `.csv`, codificação UTF-8, tamanho máximo de 2 MB e este cabeçalho exato:

```csv
nome,cpf,dataNascimento,sexo,telefone,email,ativo
```

Regras por coluna:

- `nome`: obrigatório; espaços externos são removidos e sequências internas são compactadas;
- `cpf`: pontuação é removida e o resultado deve ter 11 dígitos;
- `dataNascimento`: formato `yyyy-MM-dd` e data não futura;
- `sexo`: normalizado para `M` ou `F`;
- `telefone`: caracteres não numéricos são removidos;
- `email`: obrigatório, válido e normalizado para minúsculas;
- `ativo`: aceita `true` ou `false`; vazio significa `true`.

Linhas logicamente inválidas e duplicidades de CPF ou e-mail são filtradas. Duplicidades são verificadas tanto no arquivo atual quanto no banco. Logs de rejeição contêm somente o número da linha e um motivo sanitizado.

## Componentes e responsabilidades

| Componente | Responsabilidade |
| --- | --- |
| `PacienteImportacaoController` | Expor os endpoints HTTP e os status `202`, `400`, `404` e `409`. |
| `PacienteImportacaoService` | Validar o upload, impedir execuções simultâneas, salvar o arquivo com UUID e operar/consultar o Job. |
| `PacienteBatchConfig` | Declarar Job, Step, reader, processor, writer e executor de uma thread. |
| `PacienteCsvLineMapper` | Converter as sete colunas e preservar o número da linha. |
| `PacienteImportacaoProcessor` | Normalizar, validar e filtrar pacientes. |
| `JpaItemWriter` | Persistir no banco da aplicação principal dentro da transação do chunk. |
| `PacienteImportacaoJobListener` | Excluir o arquivo temporário somente quando a execução terminar como `COMPLETED`. |

## Endpoints

### Iniciar

```http
POST /batch/pacientes/importacoes
Content-Type: multipart/form-data
arquivo=<pacientes.csv>
```

A resposta contém o `executionId`, o nome do Job, o estado inicial, o nome original e o instante de início. O cabeçalho `Location` aponta para a consulta da execução.

### Consultar

```http
GET /batch/pacientes/importacoes/{executionId}
```

A resposta informa estado, horários, itens lidos, gravados, filtrados, ignorados e um resumo sem dados pessoais.

## Falhas e limites assumidos

- Arquivo vazio, extensão incorreta, cabeçalho diferente ou estrutura básica inválida resulta em `400`.
- Upload acima de 2 MB resulta em `400`.
- Uma nova solicitação enquanto outra execução está ativa resulta em `409`.
- Até 100 erros estruturais de leitura são ignorados e contabilizados; o próximo erro faz o Job falhar.
- Arquivos de execuções `COMPLETED` são excluídos; arquivos de execuções `FAILED` são preservados para diagnóstico.
- Uma nova submissão do mesmo arquivo gera outra execução, mas registros já existentes são filtrados pelas regras de unicidade.
- O diretório temporário é configurável por `BATCH_INPUT_DIR`.

## Evidências automatizadas

Os testes cobrem upload e endpoints, cabeçalho inválido, execução com PostgreSQL real, normalização, registros inválidos e duplicados, contadores e persistência. A validação manual usa o arquivo [pacientes-exemplo.csv](../../src/main/resources/batch/pacientes-exemplo.csv).

## Critérios de aceite

- [x] Existem Job, Step, reader, processor e writer.
- [x] O Step grava em chunks de 10.
- [x] A execução HTTP é assíncrona e consultável por `executionId`.
- [x] O CSV e as linhas passam pelas validações e normalizações definidas.
- [x] CPF e e-mail duplicados são filtrados na importação e protegidos por unicidade no banco.
- [x] O destino é o PostgreSQL da aplicação principal.
- [x] Existe arquivo de exemplo com linhas válidas, inválidas e duplicadas.
- [ ] A demonstração manual foi revisada pelo aluno.
