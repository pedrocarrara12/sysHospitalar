# Config Server

Servidor de configuracao centralizada do SysHospitalar, criado para a Etapa 3.

O projeto utiliza o profile `native` e procura os arquivos atendidos aos clientes em
`src/main/resources/config-repository`.

## Executar localmente

Na raiz do projeto SysHospitalar:

```powershell
.\mvnw.cmd -f config-server\pom.xml spring-boot:run
```

Por padrao, o servidor responde em `http://localhost:8888`. A porta pode ser alterada
com a variavel de ambiente `SERVER_PORT`.
