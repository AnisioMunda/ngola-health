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

| Variável            | Utilização                                          |
| ------------------- | --------------------------------------------------- |
| `DB_NAME`           | Nome local da base de dados PostgreSQL              |
| `DB_USER`           | Utilizador local da base de dados                   |
| `DB_PASSWORD`       | Palavra-passe local do PostgreSQL                   |
| `JWT_SECRET`        | Chave JWT em Base64, gerada com 32 bytes aleatórios |
| `REDIS_PASSWORD`    | Palavra-passe local do Redis                        |
| `RABBITMQ_USER`     | Utilizador RabbitMQ; pode usar `hospitalao`         |
| `RABBITMQ_PASSWORD` | Palavra-passe local do RabbitMQ                     |
| `DEV_ADMIN_INITIAL_PASSWORD_BASE64` | Senha temporária do `platform-admin`, codificada em Base64 |

Escolha uma senha temporária com pelo menos 12 caracteres e codifique-a para o
valor `DEV_ADMIN_INITIAL_PASSWORD_BASE64`:

```sh
printf '%s' 'a-sua-senha-temporaria' | base64 | tr -d '\n'
```

O utilizador `platform-admin` é criado apenas no perfil `dev`, recebe o papel
`SUPER_ADMIN` e tem de trocar a senha no primeiro acesso. A senha Base64 é
apenas uma codificação, não uma forma de encriptação; proteja o `.env` como
protegeria a senha original.

Gere valores novos para cada ambiente; não reutilize credenciais de produção:

```sh
openssl rand -hex 32
openssl rand -base64 32
```

Use um valor hexadecimal para cada palavra-passe de serviço e um valor Base64
para `JWT_SECRET`. Guarde os resultados apenas em `.env` e nunca os inclua em
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
| Redis                     | `localhost:6379`                            |
| RabbitMQ                  | `localhost:5672`                            |
| Gestão do RabbitMQ        | <http://localhost:15672>                    |

Use `RABBITMQ_USER` e `RABBITMQ_PASSWORD` do `.env` para entrar na consola de
gestão. O frontend encaminha `/api` e `/ws` para o backend.

Para acompanhar os registos:

```sh
docker compose logs --follow backend
docker compose logs --follow frontend
```

Para parar os serviços sem apagar os dados:

```sh
docker compose down
```

As bases de dados ficam em volumes Docker e são preservadas por esse comando.
`docker compose down --volumes` apaga os volumes e todos os dados locais; use-o
apenas quando quiser reiniciar o ambiente de forma destrutiva.

## Desenvolver o backend

Para executar o backend no computador e manter PostgreSQL, Redis e RabbitMQ em
containers:

```sh
# Na raiz do repositório
docker compose up --detach postgres redis rabbitmq
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

### Segredos da integração AGT em produção

A integração começa desactivada (`AGT_ENABLED=false`). Não a active até o
contrato técnico aplicável e a homologação estarem confirmados na ADR-0009.
Quando activada, o perfil `prod` exige `AGT_USERNAME`, `AGT_PASSWORD`,
`AGT_SOFTWARE_VALIDATION` e `AGT_PRIVATE_KEY_PATH`. Configure também
`AGT_TAX_EXEMPTION_CODE` para submeter facturas com taxa de IVA zero. Injecte
as credenciais através do gestor de segredos do
ambiente e monte a chave privada RSA em ficheiro fora da imagem e do
repositório; a aplicação valida a sua leitura e formato no arranque. Não use
`AGT_PRIVATE_KEY_CONTENT`, não inclua chaves em `.env` e não reutilize
credenciais de produção em desenvolvimento ou testes.

## Desenvolver o frontend

Com o backend local a responder na porta `8080`:

```sh
cd frontend
npm ci
npm start
```

Abra <http://localhost:4200>. O servidor de desenvolvimento usa o proxy
configurado em `src/proxy.conf.json` para encaminhar `/api` e `/ws` ao backend.

Comandos de validação disponíveis:

```sh
npm run lint
npm test
npm run build:prod
npm run format:check
```

`npm test` executa os testes em Chrome Headless. O lint pode apresentar avisos
`no-explicit-any` herdados do protótipo; erros de lint fazem o comando falhar.

## Verificações do CI

O workflow `.github/workflows/ci.yml` valida:

- Backend: `./mvnw clean verify` com Java 25 e relatório JaCoCo.
- Frontend: `npm ci`, lint, testes e build de produção com Node 26.
- Builds das imagens Docker.
- Segredos no histórico Git, através do Gitleaks.
- Caracteres gregos/cirílicos nos caminhos e no código-fonte:
  `python3 scripts/check-homoglyphs.py`.

## Resolução de problemas

- **Um serviço não fica saudável:** consulte `docker compose ps` e os registos
  com `docker compose logs --follow <serviço>`.
- **Porta ocupada:** confirme se as portas `4200`, `8080`, `5432`, `6379`,
  `5672` ou `15672` já estão a ser usadas.
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
