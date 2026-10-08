# Guia de desenvolvimento

Este guia descreve como preparar, executar, testar e depurar o Ngola Health
localmente. Os comandos assumem macOS ou Linux e são executados na raiz do
repositório, salvo indicação em contrário.

## Requisitos

- Git.
- Docker Desktop ou Docker Engine com o plugin Docker Compose.
- Java 25 para compilar e executar o backend directamente no computador.
- Node.js 26 e npm 11 para desenvolver o frontend directamente no computador.
- OpenSSL para gerar credenciais locais.

Para executar a aplicação completa em containers, basta ter o Docker com
Compose: as imagens compilam o backend com Java 25 e o frontend com Node 26.

## Preparar o ambiente

Clone o repositório e crie a configuração local:

```sh
git clone https://github.com/AnisioMunda/ngola-health.git
cd ngola-health
cp .env.example .env
```

Preencha o ficheiro `.env`, que é ignorado pelo Git:

| Variável                            | Utilização                                                 |
| ----------------------------------- | ---------------------------------------------------------- |
| `DB_NAME`                           | Nome local da base de dados PostgreSQL                     |
| `DB_USER`                           | Utilizador local da base de dados                          |
| `DB_PASSWORD`                       | Palavra-passe local do PostgreSQL                          |
| `JWT_SECRET`                        | Chave JWT em Base64, gerada com 32 bytes aleatórios        |
| `DEV_ADMIN_INITIAL_PASSWORD_BASE64` | Senha temporária do `platform-admin`, codificada em Base64 |
| `AGT_ENABLED`                       | `true` para testar a integração no sandbox da AGT; por omissão, `false` |
| `AGT_USERNAME` / `AGT_PASSWORD`     | Credenciais fornecidas para homologação; nunca use credenciais de produção |
| `AGT_PRIVATE_KEY_PATH`              | Caminho local para a chave de teste, se exigida pela conta de homologação |
| `AGT_NIF`, `AGT_SOFTWARE_ID`, `AGT_SOFTWARE_VERSION` | Identificação fiscal e do software usada no sandbox |
| `AGT_SOFTWARE_VALIDATION`, `AGT_TAX_EXEMPTION_CODE` | Dados opcionais exigidos pelo fluxo de homologação |
| `BACKUP_AGE_RECIPIENT`              | Chave pública `age` para cifrar backups locais (opcional) |
| `BACKUP_RETENTION_DAYS`             | Retenção dos backups locais; padrão de 30 dias |

Escolha uma senha temporária com pelo menos 12 caracteres e codifique-a para o
valor `DEV_ADMIN_INITIAL_PASSWORD_BASE64`:

```sh
printf '%s' 'a-sua-senha-temporaria' | base64 | tr -d '\n'
```

O utilizador `platform-admin` é criado apenas no perfil `dev`, recebe o papel
`SUPER_ADMIN` e tem de trocar a senha no primeiro acesso. A senha Base64 é
apenas uma codificação, não uma forma de encriptação; proteja o `.env` como
protegeria a senha original.

Os dados de demonstração da aplicação são mantidos nos changesets Liquibase
com contexto `@dev`, em
[`004-dev-seed.sql`](../backend/src/main/resources/db/changelog/changes/users/004-dev-seed.sql).
Não importe scripts SQL manuais de seed. As fixtures Playwright são separadas,
sintéticas e exclusivas da base E2E.

Gere valores novos para cada ambiente; não reutilize credenciais de produção:

```sh
openssl rand -hex 32
openssl rand -base64 32
```

Use um valor hexadecimal para `DB_PASSWORD` e um valor Base64 para
`JWT_SECRET`. Guarde os resultados apenas em `.env` e nunca os inclua em
commits, registos ou pedidos de suporte.

Valide a configuração sem imprimir os valores:

```sh
docker compose config --quiet
```

## Executar a aplicação completa

Na raiz do repositório:

```sh
docker compose up --build --detach
docker compose ps
```

Espere que os serviços indiquem o estado `healthy`. A interface e os serviços
ficam disponíveis apenas no computador local:

| Serviço                   | Endereço                                    |
| ------------------------- | ------------------------------------------- |
| Interface Angular         | <http://localhost:4200>                     |
| API e health check        | <http://localhost:8080/api/actuator/health> |
| Swagger UI (perfil `dev`) | <http://localhost:8080/api/swagger-ui.html> |
| PostgreSQL                | `localhost:5432`                            |
O frontend encaminha `/api` para o backend. O stack local necessita apenas de
PostgreSQL, backend e frontend; não inicia serviços de cache, fila ou storage
externo.

Para acompanhar os registos:

```sh
docker compose logs --follow backend
docker compose logs --follow frontend
```

### Backups locais

Os backups são opcionais e ficam no directório `backups/` da raiz do projecto,
ignorado pelo Git. Não é necessária conta S3. A rotina cifra os dumps com
`age`; instale-o localmente e crie uma identidade cuja chave privada fique fora
do projecto:

```sh
mkdir -p "$HOME/.config/age"
chmod 700 "$HOME/.config/age"
age-keygen -o "$HOME/.config/age/hospitalao-backup.txt"
chmod 600 "$HOME/.config/age/hospitalao-backup.txt"
age-keygen -y "$HOME/.config/age/hospitalao-backup.txt"
```

Copie o recipient público apresentado para `BACKUP_AGE_RECIPIENT` no `.env`.
Nunca copie a identidade privada para `backups/` nem a partilhe. Para gerar um
backup imediato:

```sh
docker compose --profile backup run --build --rm backup /usr/local/bin/backup.sh
```

Configure primeiro um recipient público válido para activar o perfil. Para
manter a rotina diária das 02:00 UTC activa enquanto desenvolve:

```sh
docker compose --profile backup up --build --detach backup
docker compose --profile backup logs --follow backup
```

Os ficheiros `.dump.age` permanecem em `backups/` até ultrapassarem a retenção
configurada. Preserve a identidade privada: sem ela, os backups cifrados não
podem ser restaurados. O perfil `backup` não é iniciado pelo `docker compose up`
normal.

Para testar o restauro, crie primeiro uma base vazia no PostgreSQL local:

```sh
docker compose exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" hospitalao_restore'
```

Escolha o nome de um ficheiro existente em `backups/` e execute:

```sh
docker compose --profile backup run --rm --no-deps \
  -v "$HOME/.config/age/hospitalao-backup.txt:/run/backup/identity.txt:ro" \
  -e BACKUP_AGE_IDENTITY_PATH=/run/backup/identity.txt \
  backup /usr/local/bin/restore.sh \
  hospitalao-YYYYMMDDThhmmssZ.dump.age hospitalao_restore
```

O destino tem de existir e estar vazio; o script recusa restaurar sobre a base
de origem configurada.

Para parar os serviços sem apagar os dados:

```sh
docker compose down
```

As bases de dados ficam em volumes Docker e são preservadas por esse comando.
`docker compose down --volumes` apaga os volumes e todos os dados locais; use-o
apenas quando quiser reiniciar o ambiente de forma destrutiva.

## Compose de produção (opcional)

O uso local normal não precisa deste Compose. A aplicação de desenvolvimento
acima serve HTTP apenas em `localhost`; só use o procedimento seguinte ao
preparar uma implantação com domínio público.

O Compose de produção não publica as portas da base de dados ou backend. O
Nginx é o único serviço exposto (portas 80 e 443), redirecciona
HTTP para HTTPS e serve o desafio ACME. Antes de começar, configure no `.env`
um domínio público (`TLS_DOMAIN`), o e-mail (`CERTBOT_EMAIL`) e os segredos e
parâmetros reais exigidos pelo backend. O DNS do domínio deve apontar para o
servidor e as portas TCP 80 e 443 devem estar acessíveis pela Internet.

Emita o certificado inicial através do perfil de bootstrap:

```sh
docker compose -f docker-compose.prod.yml --profile bootstrap run --build --rm certbot-init
docker compose -f docker-compose.prod.yml --profile bootstrap down
docker compose -f docker-compose.prod.yml up --build --detach --wait
```

O perfil `bootstrap` serve o desafio HTTP sem expor a aplicação como
implantação final. Depois de obter o certificado, o Compose normal inicia o
Nginx TLS e um serviço Certbot que verifica renovações a cada 12 horas; após
uma renovação, o Nginx recebe um reload gracioso. Mantenha os volumes
`letsencrypt` e `certbot_webroot`: removê-los elimina os certificados e exige
uma nova emissão. Não use `down --volumes` numa implantação com dados reais.

O perfil `prod` do backend não cria o utilizador administrativo de demonstração
e mantém a documentação Swagger desactivada. Configure os valores reais de
`AGT_API_URL`, `AGT_NIF`, `AGT_SOFTWARE_ID` e `AGT_SOFTWARE_VERSION`; se a
integração AGT for activada, `AGT_PRIVATE_KEY_PATH` deve apontar para a chave
PEM existente no host. O Compose monta-a em modo apenas de leitura.

## Benchmark de consultas PostgreSQL

O item 8.7 foi medido num PostgreSQL 18 isolado com dados sintéticos de um
hospital: 50 000 pacientes, 250 000 consultas, 500 000 episódios e 250 000
facturas. As estruturas e índices de base correspondem às migrations actuais;
as medições usam `EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)`. Resultados de uma
execução local no Docker Desktop:

| Consulta representativa | Sem índice candidato | Com índice candidato | Plano observado |
| --- | ---: | ---: | --- |
| Pesquisa de paciente por substring (`LIKE '%termo%'`) | 47,526 ms | 1,325 ms | `Seq Scan` → `BitmapOr` com GIN trigram |
| Agenda de médico por intervalo de datas | 0,980 ms | 0,227 ms | índice data/médico → índice médico/data/hora |
| Página de episódios por médico e estado | 5,678 ms | 0,136 ms | bitmap + ordenação → leitura ordenada do índice composto |
| Facturas por hospital e estado, mais recentes primeiro | 41,824 ms | 0,117 ms | varrimento paralelo → índice composto com limite |

Os tempos dependem da cache e do hardware e não constituem um SLA. Os índices
GIN aumentam o espaço e o custo de escrita, mas preservam a pesquisa parcial
actual; os índices B-tree compostos seguem filtros e ordenações usados pelos
repositórios. As novas migrations criam índices com `CONCURRENTLY` para evitar
bloquear escritas durante a construção.

## Desenvolver o backend

Para executar o backend no computador e manter PostgreSQL em container:

```sh
# Na raiz do repositório
docker compose up --detach postgres
set -a
. ./.env
set +a

cd backend
./mvnw -version
./mvnw clean verify
```

Os testes de integração usam Testcontainers e precisam de um daemon Docker
activo. O relatório JaCoCo é criado em
`backend/target/site/jacoco/index.html`.

Para executar a aplicação a partir do IDE ou do terminal, mantenha as variáveis
do `.env` exportadas, use Java 25 e active o perfil `dev`:

```sh
SPRING_PROFILES_ACTIVE=dev \
SPRING_JPA_HIBERNATE_DDL_AUTO=none \
./mvnw spring-boot:run
```

O `docker-compose.yml` publica os serviços internos apenas em `127.0.0.1`,
permitindo que o backend local lhes aceda sem os expor à rede. O override
`SPRING_JPA_HIBERNATE_DDL_AUTO=none` é temporário: existe uma incompatibilidade
conhecida entre algumas entidades do protótipo e as migrações. O Liquibase
continua a gerir o esquema; o teste de arranque valida as migrações numa base
PostgreSQL limpa.

No IDE, configure `ao.hospitalao.HospitalAoApplication` como classe principal,
Java 25 como JDK e as mesmas variáveis de ambiente. Para ligar um debugger Java
remoto na porta `5005`, acrescente ao comando Maven:

```sh
SPRING_PROFILES_ACTIVE=dev \
SPRING_JPA_HIBERNATE_DDL_AUTO=none \
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

O backend fica em `http://localhost:8080`; o health check é
`http://localhost:8080/api/actuator/health`.

### Integração AGT no sandbox local

A configuração local aponta exclusivamente para
`https://sifphml.minfin.gov.ao` e define `AGT_SANDBOX=true`. A integração
continua desligada até configurar credenciais autorizadas de homologação:
defina `AGT_ENABLED=true`, `AGT_USERNAME` e `AGT_PASSWORD` no `.env`; se o
fluxo de assinatura exigir uma chave, indique em `AGT_PRIVATE_KEY_PATH` um
ficheiro de teste fora do repositório. O Compose monta-o apenas para leitura.
Não use credenciais nem chaves de produção.

### Fluxo e homologação AGT

As páginas públicas consultadas descrevem `POST /sigt/fe/v1/registarFactura`
para submissão assíncrona e `POST /sigt/fe/v1/obterEstado` para consulta do
`requestID`. O cliente submete um documento por pedido; o serviço agendado
consulta facturas pendentes a cada dois minutos. Falhas de rede e respostas
HTTP 5xx têm até três tentativas; respostas 4xx, incluindo o HTTP 429 da AGT,
não são repetidas automaticamente.

O Compose local não permite substituir esse endereço por uma URL de produção.
Este ambiente é apropriado para experimentação com dados sintéticos e
credenciais de teste; as respostas do sandbox não comprovam conformidade nem
certificação. O contrato, o mapeamento SAF-T, as assinaturas e o processo de
homologação ainda têm limitações documentadas na
[ADR-0009](./adr/0009-agt-invoicing.md). Não envie dados reais de pacientes
para testes.

O PDF só incorpora QR quando a factura está aceite e existe um payload
fornecido pela AGT. O esquema público de consulta ainda não confirma esse
payload nem o código de validação; até essa confirmação, o sistema não inventa
um QR local e assinala documentos não validados.

## Desenvolver o frontend

Com o backend local a responder na porta `8080`:

```sh
cd frontend
npm ci
npm start
```

Abra <http://localhost:4200>. O servidor de desenvolvimento usa o proxy
configurado em `src/proxy.conf.json` para encaminhar `/api` ao backend.

Comandos de validação disponíveis:

```sh
npm run lint
npm test
npm run build:prod
npm run format:check
```

`npm test` executa os testes unitários com Vitest e jsdom. O lint pode
apresentar avisos `no-explicit-any` herdados do protótipo; erros de lint fazem
o comando falhar.

### Teste integrado com Playwright

O fluxo E2E usa PostgreSQL descartável, inicia o backend e o
frontend e percorre, pela interface, o login, o cadastro de paciente, o
episódio, a prescrição e a factura. O setup cria um administrador, um
medicamento com stock e um preço de serviço sintéticos; o paciente e os
registos clínicos são criados pelo próprio teste. Não aponta para a base de
dados normal do desenvolvimento nem usa dados reais.

Com Java 25, Node 26, dependências do frontend e Docker activo:

```sh
docker compose -f docker-compose.e2e.yml up --detach --wait
cd frontend
npx playwright install chromium
npm run e2e
```

O Playwright usa a porta local `5433` (PostgreSQL) e inicia a aplicação em
`4200`/`8080`. A porta e as credenciais da base de dados são ajustáveis pelas
variáveis `E2E_DB_*`. O CI executa o
mesmo fluxo com serviços efémeros. Para remover os serviços locais e os dados
sintéticos, execute:

```sh
docker compose -f docker-compose.e2e.yml down
```

## Verificações do CI

O workflow `.github/workflows/ci.yml` valida:

- Backend: `./mvnw clean verify` com Java 25 e relatório JaCoCo.
- Frontend: `npm ci`, lint, testes e build de produção com Node 26.
- Dependências: `npm audit --audit-level=high` e revisão de dependências novas
  nos pull requests; Dependabot verifica semanalmente npm, Maven e GitHub Actions.
- Builds das imagens Docker.
- OWASP ZAP Baseline contra frontend e backend em ambiente E2E isolado, em cada
  pull request, push para `main` e semanalmente; o relatório é guardado como
  artefacto do workflow. O ficheiro `.zap/rules.tsv` mantém como informativos
  o cache intencional da shell SPA, os estilos runtime exigidos pelo Angular e
  a detecção esperada de aplicação Angular.
- Segredos no histórico Git, através do Gitleaks.
- Caracteres gregos/cirílicos nos caminhos e no código-fonte:
  `python3 scripts/check-homoglyphs.py`.

Para executar o baseline localmente com Docker activo, use um projecto Compose
descartável:

```sh
docker compose -f docker-compose.e2e.yml --profile zap up --build --detach --wait
docker run --rm --add-host=host.docker.internal:host-gateway \
  ghcr.io/zaproxy/zaproxy:stable zap-baseline.py \
  -t http://host.docker.internal:4201
docker compose -f docker-compose.e2e.yml --profile zap down --volumes --remove-orphans
```

## Resolução de problemas

- **Um serviço não fica saudável:** consulte `docker compose ps` e os registos
  com `docker compose logs --follow <serviço>`.
- **Porta ocupada:** confirme se as portas `4200`, `8080` ou `5432` já estão a
  ser usadas.
- **Compose pede uma variável:** confirme que `.env` existe e que todas as
  variáveis obrigatórias estão preenchidas.
- **Erro de formato do JWT:** gere `JWT_SECRET` com
  `openssl rand -base64 32`; não use uma frase normal como chave.
- **API indisponível através do frontend:** confirme o health check do backend
  e consulte `docker compose logs --follow backend`.

O histórico Git da branch `main` foi reescrito para remover um segredo
anteriormente exposto. Clones criados antes dessa alteração devem ser
descartados e clonados novamente; não publique branches antigas.

Este ambiente Compose destina-se ao desenvolvimento. Não use dados reais de
pacientes nem reutilize credenciais locais em produção.
