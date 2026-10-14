# Sistema de Partituras

Busca e navegação de partituras armazenadas em uma pasta do Google Drive.

## Configuração

1. No Google Cloud, crie uma *service account*, gere uma chave JSON e compartilhe
   a pasta do acervo com o e-mail da service account (permissão de leitor).
2. Salve a chave em `config/google-credentials.json` (a pasta `config/` está no
   `.gitignore` — **nunca** coloque a chave em `src/main/resources`).
   Alternativamente, aponte a variável `GOOGLE_APPLICATION_CREDENTIALS` para o arquivo.
3. Opcional: defina `PARTITURAS_FOLDER_ID` com o ID da pasta raiz.

| Propriedade                    | Padrão                          |
|--------------------------------|---------------------------------|
| `partituras.folder-id`         | `$PARTITURAS_FOLDER_ID`         |
| `partituras.credentials-path`  | `config/google-credentials.json`|
| `partituras.cache-ttl`         | `10m`                           |

## Executar

```sh
./mvnw spring-boot:run
```

Acesse http://localhost:8080. O acervo é lido do Drive uma vez e mantido em cache
pelo tempo de `partituras.cache-ttl`; arquivos novos aparecem após esse intervalo.

## Testes

```sh
./mvnw test
```
