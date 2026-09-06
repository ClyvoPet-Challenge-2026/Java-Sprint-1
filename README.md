# ClyvoCare API - Cadastros e Contratação

API Java do projeto **ClyvoCare**, desenvolvida como parte do Challenge FIAP 2026 (2TDSPG, Java Advanced). É a metade administrativa de um sistema de plano de saúde para pets inspirado em serviços como o Petlove Saúde, cuidando do cadastro de tutores, pets, planos disponíveis e das contratações desses planos.

O projeto tem uma irmã em C# (.NET Advanced) que cuida da operação clínica em si (clínicas, eventos clínicos, lembretes). As duas APIs compartilham o mesmo banco Oracle FIAP via um schema que serve como contrato entre elas, cada uma sendo dona da escrita das suas próprias tabelas e apenas lendo as do outro contexto quando precisa de informações para exibir.

---

## Índice

- [Sobre a divisão por domínio](#sobre-a-divisão-por-domínio)
- [Stack técnica](#stack-técnica)
- [Benefícios para o Negócio](#benefícios-para-o-negócio)
- [Arquitetura Macro na Nuvem](#arquitetura-macro-na-nuvem)
- [Como executar localmente](#como-executar-localmente)
- [Como executar na nuvem (Azure + Docker)](#como-executar-na-nuvem-azure--docker)
- [Arquitetura da aplicação](#arquitetura-da-aplicação)
- [Autenticação e Autorização](#autenticação-e-autorização)
- [Endpoints principais](#endpoints-principais)
- [Testando localmente](#testando-localmente)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Schema do banco](#schema-do-banco)
- [Requisitos da disciplina cobertos](#requisitos-da-disciplina-cobertos)
- [Limitações conhecidas](#limitações-conhecidas)
- [Próximos passos](#próximos-passos)

---

## Sobre a divisão por domínio

A escolha foi separar as duas APIs por **bounded context** (contexto delimitado), e não por questões técnicas como performance. A ideia é simples: uma API cuida de quem somos (cadastros estáveis), a outra cuida do que fazemos pelo pet (jornada de cuidado). Essa fronteira fica explícita no nome dos pacotes, na estrutura do banco e nos pacotes que cada lado pode escrever.

Esta API (Java) é dona de escrita das seguintes tabelas:

- `TB_CAD_OWNER` — tutores dos pets
- `TB_CAD_PET` — os pets em si
- `TB_CAD_PLAN` — catálogo de planos (Essential, Basic, Premium, Master, Total)
- `TB_CAD_SUBSCRIPTION` — contratações de plano feitas pelos tutores
- `TB_CAD_SPECIES`, `TB_CAD_BREED` — taxonomia de espécies e raças
- `TB_CAD_STATE`, `TB_CAD_CITY` — localização
Status e forma de pagamento são enums armazenados nas colunas `STATUS` e `PAYMENT_METHOD` de `TB_CAD_SUBSCRIPTION`, sem tabelas auxiliares.

A API C# escreve em `TB_CAD_CLINIC`, `TB_HEA_CLINICAL_EVENT` e `TB_HEA_REMINDER`, e lê algumas das tabelas acima quando precisa.

---

## Stack técnica

| Camada             | Tecnologia                                                    |
| ------------------ | ------------------------------------------------------------- |
| Linguagem          | Java 23                                                       |
| Framework          | Spring Boot 4.0.6                                             |
| Persistência       | Spring Data JPA + Hibernate 7                                 |
| Banco de dados     | Oracle XE 21 (nuvem) / Oracle 19c FIAP (local)               |
| Driver JDBC        | ojdbc11                                                       |
| Validação          | Bean Validation (Hibernate Validator + extensões BR para CPF) |
| Cache              | Spring Cache (in-memory)                                      |
| Migrations         | Flyway                                                        |
| Autenticação       | JWT stateless assinado com RSA (RS256), via Spring Security   |
| Segurança de senha | Spring Security (`BCryptPasswordEncoder`)                     |
| Documentação       | SpringDoc OpenAPI (Swagger UI)                                |
| Boilerplate        | Lombok                                                        |
| Build              | Maven                                                         |
| Containerização    | Docker + Docker Compose                                       |
| Nuvem              | Microsoft Azure (VM Ubuntu 24.04)                             |
| Banco em container | Oracle XE 21-slim (gvenzl/oracle-xe)                         |

---

## Benefícios para o Negócio

O ClyvoCare resolve um problema real do mercado pet brasileiro: a fragmentação e informalidade no acompanhamento de saúde dos animais. Ao digitalizar o cadastro de tutores, pets, planos e contratações em uma API robusta e escalável, o sistema oferece:

- **Gestão centralizada** de toda a base de clientes e seus pets em um único sistema
- **Rastreabilidade completa** do histórico de contratações por pet, permitindo análises de churn e upsell
- **Flexibilidade de planos** com 5 tiers (Essential a Total), cobrindo diferentes perfis de tutor e pet
- **Segurança de dados** com senhas hasheadas em BCrypt e validação rigorosa de CPF e e-mail
- **Escalabilidade na nuvem** via containerização Docker na Azure, permitindo crescimento sem reconfiguração de infraestrutura
- **Integração nativa** com a API clínica (.NET), formando um ecossistema completo de saúde pet

---

## Arquitetura Macro na Nuvem

```
Usuário / Browser / Insomnia
            |
       HTTP :8080
            |
   Microsoft Azure — East US 2
   ┌─────────────────────────────────────────┐
   │  VM: vm-clyvo (Standard_D2s_v3)         │
   │  Ubuntu 24.04 LTS                        │
   │                                          │
   │  ┌──────────────────────────────────┐   │
   │  │  Docker Engine                   │   │
   │  │                                  │   │
   │  │  ┌─────────────┐  JDBC :1521     │   │
   │  │  │ clyvocare-  │ ──────────────► │   │
   │  │  │ api         │  ┌────────────┐ │   │
   │  │  │             │  │ clyvocare- │ │   │
   │  │  │ Spring Boot │  │ oracle     │ │   │
   │  │  │ Java 23     │  │            │ │   │
   │  │  │ porta 8080  │  │ Oracle XE  │ │   │
   │  │  │ user:appuser│  │ 21-slim    │ │   │
   │  │  └─────────────┘  │ porta 1521 │ │   │
   │  │                   └─────┬──────┘ │   │
   │  │                         │        │   │
   │  │              ┌──────────▼──────┐ │   │
   │  │              │ Volume nomeado  │ │   │
   │  │              │ oracle-data     │ │   │
   │  │              │ (persistência)  │ │   │
   │  │              └─────────────────┘ │   │
   │  └──────────────────────────────────┘   │
   └─────────────────────────────────────────┘
```

> O diagrama visual completo (Draw.io) está disponível em `docs/Arquitetura_DevOps.drawio`.

---

## Como executar localmente

Pré-requisitos no ambiente:

- Java 17 instalado e disponível no `PATH`
- OpenSSL instalado (para gerar as chaves JWT)
- Acesso à VPN da FIAP (necessário para alcançar `oracle.fiap.com.br`)
- Credenciais do banco Oracle FIAP (RM e senha)
- Schema Oracle acessível com o seu RM — o Flyway aplica V1 + V2 em um banco vazio; `docs/script.sql` recria o schema com 13 tabelas e dados de demonstração (ver [Schema do banco](#schema-do-banco))

Gere o par de chaves RSA usado para assinar o JWT (não são versionadas — cada ambiente gera a sua):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/keys/private_key.pem
openssl rsa -pubout -in src/main/resources/keys/private_key.pem -out src/main/resources/keys/public_key.pem
```

Rodando a aplicação:

```bash
./mvnw spring-boot:run
```

Por padrão a API sobe na porta `8080`. Se você precisar de outra porta, sobrescreve com `--server.port=XXXX` ou edita o `application.properties`.

Para confirmar que o banco conectou direito, no log da inicialização deve aparecer algo parecido com:

```
HikariPool-1 - Added connection oracle.jdbc.driver.T4CConnection
Database JDBC URL [jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL]
Database dialect: OracleDialect
```

---

## Como executar na nuvem (Azure + Docker)

> Estas instruções de VM são da Sprint 1. A entrega de DevOps da Sprint 3 exige ACR + ACI ou App Service, conforme o enunciado. O provisionamento ainda precisa ser adaptado.

### Pré-requisitos

- [Azure CLI](https://aka.ms/installazurecliwindows) instalado
- Conta Azure com subscription ativa
- Docker instalado localmente (apenas para testes locais)

### 1. Login na Azure

```bash
az login
az account set --subscription "NOME_OU_ID_DA_SUBSCRIPTION"
```

### 2. Executar o script de provisionamento

O script `azure-setup.sh` na raiz do projeto realiza automaticamente todas as etapas de infraestrutura:

```bash
chmod +x azure-setup.sh
./azure-setup.sh
```

O script executa em sequência:

1. **Cria o Resource Group** `sprint1-javaapi-clyvo` na região `East US 2`
2. **Provisiona a VM Linux** `vm-clyvo` (Standard_D2s_v3 — Ubuntu 24.04 LTS)
3. **Abre as portas necessárias** — porta 8080 (API) e 22 (SSH já aberta por padrão)
4. **Instala o Docker** na VM via script oficial
5. **Instala Git e ferramentas** (nano, curl, wget, unzip)
6. **Clona o repositório** e cria o `docker-compose.yml` na VM
7. **Sobe os containers** com `docker compose up --build -d`

Ao final o script exibe o IP público da VM e o link do Swagger.

### 3. Conectar na VM

```bash
ssh admclyvo@<IP_DA_VM>
```

### 4. Verificar os containers

```bash
cd Java-Sprint-1
docker ps
docker volume ls
docker inspect clyvocare-api --format '{{.Config.User}}'
```

### 5. Acessar o Swagger

```
http://<IP_DA_VM>:8080/swagger-ui.html
```

### 6. Demonstrar persistência de dados

```bash
# Para os containers sem destruí-los
docker compose stop

# Inicia novamente
docker compose start
```

> Aguarde ~3-5 minutos para o Oracle XE reinicializar. Os dados inseridos anteriormente estarão preservados graças ao volume nomeado `java-sprint-1_oracle-data`.

### 7. Excluir a VM ao final (obrigatório)

```bash
az group delete --name sprint1-javaapi-clyvo --yes --no-wait
```

> ⚠️ Este comando remove todos os recursos criados: VM, disco, IP público, rede e NSG.

---

## Containerização — Dockerfile

O `Dockerfile` na raiz do projeto usa multi-stage build para manter a imagem final leve:

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# Usuário sem privilégios administrativos
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> A API roda com usuário `appuser` sem privilégios root, atendendo ao requisito de segurança da disciplina.

---

## Arquitetura da aplicação

A API segue uma arquitetura em camadas bem tradicional do mundo Spring, sem muita ginástica arquitetural — o foco é clareza e cobertura dos requisitos da disciplina, não criar abstrações desnecessárias.

```
Controller (REST)  -->  Service (lógica + cache + validação de FK)  -->  Repository (JPA)  -->  Oracle
       |                          |
       v                          v
     DTOs                     Entities
```

A regra prática: o Controller só conhece DTOs (Request e Response), o Service trabalha com entidades JPA e regras de negócio, o Repository fala com o banco. A conversão entre DTO e Entity acontece nas bordas (`request.toEntity(...)` na entrada, `Response.fromEntity(entity)` na saída), seguindo um padrão de records imutáveis.

**Senhas com BCrypt.** Toda senha de tutor passa pelo `PasswordEncoder` do Spring Security (`BCryptPasswordEncoder`) antes de ser salva. O campo `passwordHash` da entidade tem `@JsonIgnore`, então mesmo se alguém esquecer de filtrar no DTO, a senha nunca vaza no JSON de resposta.

**Cache nas listagens estáveis.** Estados e Cidades são marcados com `@Cacheable`. Quando alguém faz POST/PUT/DELETE nesses recursos, `@CacheEvict` limpa tudo. Para entidades de negócio (Owner, Pet, Subscription) não usamos cache porque o volume de mudanças é maior e o risco de servir dado stale supera o benefício.

**Validação de FK no Service, não no banco.** Antes de inserir uma Cidade, por exemplo, o `CityService` consulta o `StateService` para garantir que o estado informado existe. Se não existe, lança 404 com mensagem clara em vez de deixar o Oracle gritar `ORA-02291`.

**Tratamento global de erros.** A classe `ValidationHandler` (anotada com `@RestControllerAdvice`) captura dois tipos de exceção e devolve JSON consistente:

- `MethodArgumentNotValidException` (erros de Bean Validation) → 400 com lista `[{field, message}]`
- `ResponseStatusException` (404s do `findById`, 409s de conflito) → 400/404/409 com `{status, message}`

**Paginação onde faz sentido.** Lookups pequenos (Estados, Espécies, Planos) devolvem `List<T>` direto. Já as entidades de negócio (Owner, Pet, Subscription) usam `Pageable` no GET, com filtros opcionais via query params.

---

## Autenticação e Autorização

A API usa **JWT stateless** (sem sessão/cookie), via Spring Security + OAuth2 Resource Server. A escolha é deliberada: o frontend roda em repositório e domínio separados, e sessão/cookie exige mesma origem — o token no header `Authorization` não tem essa restrição.

O JWT é assinado com **RSA (RS256)**: a API guarda um par de chaves em `src/main/resources/keys/`, o `AuthController` assina com a privada no login, e o `SecurityConfig` valida com a pública em cada request. As chaves não são versionadas — cada ambiente gera o próprio par (ver [Como executar localmente](#como-executar-localmente)).

### Login

```
POST /auth/login
Content-Type: application/json

{
    "email": "carlos@email.com",
    "password": "senha123"
}
```

Resposta:

```json
{ "token": "eyJhbGciOiJSUzI1NiJ9..." }
```

Use o token nas próximas requisições: `Authorization: Bearer <token>`.

### Perfis e proteção de rotas

`TB_CAD_OWNER.ROLE_NAME` guarda o perfil do tutor (`ADMIN` ou `OWNER`). O token carrega esse valor na claim `role`; o Spring Security o expõe como a authority `ROLE_ADMIN` / `ROLE_OWNER`, e cada endpoint declara quem pode acessá-lo via `@PreAuthorize`.

| Recurso | GET | POST | PUT | DELETE |
|---|---|---|---|---|
| `/responsaveis` | ADMIN | público (auto-cadastro) | ADMIN | ADMIN |
| `/pets` | qualquer logado | ADMIN ou OWNER | ADMIN ou OWNER | ADMIN ou OWNER |
| `/contratacoes` | qualquer logado | ADMIN ou OWNER | ADMIN | ADMIN |
| Lookups (planos, estados, cidades, espécies, raças) | qualquer logado | ADMIN | ADMIN | ADMIN |
| `/status-contratacao`, `/formas-pagamento` | qualquer logado | — | — | — |

`/auth/login` e o Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) são as únicas rotas públicas, além do `POST /responsaveis`.

> CORS ainda não está configurado — ver [Limitações conhecidas](#limitações-conhecidas).

---

## Status e forma de pagamento

Os dois campos usam `@Enumerated(EnumType.STRING)` e colunas `VARCHAR2(20)` na contratação, protegidas por constraints no Oracle:

| Campo JSON / coluna Oracle | Enum Java | Valores |
|---|---|---|
| `status` / `STATUS` | `SubscriptionStatus` | `ACTIVE`, `INACTIVE`, `PENDING` |
| `paymentMethod` / `PAYMENT_METHOD` | `PaymentMethod` | `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`, `PIX` |

`ACTIVE` indica contratação ativa; `INACTIVE`, encerrada ou desativada; `PENDING`, aguardando confirmação ou regularização.

- POST e PUT recebem os dois campos como strings, substituindo `statusId` e `paymentMethodId`. As respostas também devolvem strings.
- Filtros: `GET /contratacoes?status=ACTIVE&paymentMethod=PIX`.
- `GET /status-contratacao` e `GET /formas-pagamento` retornam listas fixas, sem consultar o banco. Essas rotas não possuem criação, edição, exclusão ou consulta por ID.
- Campos ausentes, nulos, desconhecidos ou numéricos retornam HTTP 400.

O cálculo em `ContractPricingService` e `FN_CALCULATE_CONTRACT_VALUE` mantém **5% para PIX e 3% para cartão de débito**; crédito e boleto não têm desconto. A integração desse cálculo ao cadastro e as regras de transição/troca de plano continuam como a próxima etapa dos fluxos de negócio. O CRUD atual ainda recebe o status no POST/PUT e usa o valor mensal do plano no cadastro.

A V2 foi aplicada e validada no Oracle FIAP, preservando as 11 contratações existentes. A inicialização do Spring/Hibernate com o schema migrado também foi conferida.

---

## Endpoints principais

Documentação completa interativa está no Swagger UI em `http://localhost:8080/swagger-ui.html`. Exceto `POST /auth/login` e `POST /responsaveis`, todo endpoint abaixo exige `Authorization: Bearer <token>` (ver [Autenticação e Autorização](#autenticação-e-autorização)).

### Cadastro de um responsável (tutor do pet)

```
POST /responsaveis
Content-Type: application/json

{
    "name": "Ana Paula Souza",
    "cpf": "529.982.247-25",
    "email": "ana@email.com",
    "password": "minhasenha123",
    "phone": "(11) 91111-1111",
    "cityId": 1
}
```

### Listar responsáveis com busca e paginação

```
GET /responsaveis?name=ana&page=0&size=10&sort=name,asc
```

### Cadastro de um pet

```
POST /pets
Content-Type: application/json

{
    "name": "Rex",
    "birthDate": "2020-03-10",
    "sex": "MALE",
    "ownerId": 1,
    "speciesId": 1,
    "breedId": 1
}
```

### Contratação de um plano

```
POST /contratacoes
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "status": "ACTIVE",
    "paymentMethod": "PIX"
}
```

### Trocar status de uma contratação

```
PUT /contratacoes/1
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "status": "INACTIVE",
    "paymentMethod": "PIX"
}
```

### Filtros úteis em contratações

```
GET /contratacoes?status=ACTIVE        # todas ativas
GET /contratacoes?petId=1           # histórico de contratações de um pet
GET /contratacoes?planId=5&status=INACTIVE  # contratações inativas do plano Total
```

### Lookups (Estados, Cidades, Espécies, etc)

- `GET /estados`
- `GET /cidades`
- `GET /especies`
- `GET /racas`
- `GET /planos`
- `GET /formas-pagamento` — lista fixa dos valores do enum; sem CRUD
- `GET /status-contratacao` — lista fixa dos valores do enum; sem CRUD

CRUD completo (POST, PUT, DELETE) existe nos lookups persistidos para operações administrativas. Os status e as formas de pagamento são fixos e possuem apenas consulta.

---

## Testando localmente

A pasta `docs/` contém:

- `clyvo-care-api.yaml` — collection completa do Insomnia
- `MER.png` — diagrama entidade-relacionamento
- `script.sql` — schema de referência (13 tabelas) + seeds de demonstração + PL/SQL da disciplina de Database
- `Arquitetura_DevOps.drawio` — diagrama de arquitetura na nuvem

Para um teste end-to-end rápido:

1. Crie um Estado via `POST /estados`
2. Crie uma Cidade via `POST /cidades` referenciando o `stateId`
3. Crie um Owner via `POST /responsaveis` referenciando o `cityId`
4. Crie uma Espécie via `POST /especies`
5. Crie uma Raça via `POST /racas` referenciando o `speciesId`
6. Crie um Pet via `POST /pets` referenciando `ownerId`, `speciesId` e `breedId`
7. Crie um Plano via `POST /planos`
8. Consulte os valores em `GET /formas-pagamento`
9. Consulte os valores em `GET /status-contratacao`
10. Crie uma Contratação via `POST /contratacoes` com `"status": "ACTIVE"` e `"paymentMethod": "PIX"`
11. Liste via `GET /contratacoes?status=ACTIVE` e verifique a persistência

---

## Estrutura de pastas

```
Java-Sprint-1/
├── Dockerfile                          ← build multi-stage da API
├── azure-setup.sh                      ← script de provisionamento Azure CLI
├── .gitignore                          ← docker-compose.yml excluído por segurança
├── pom.xml
├── mvnw / mvnw.cmd
├── docs/
│   ├── clyvo-care-api.yaml
│   ├── script.sql
│   ├── MER.png
│   └── Arquitetura_DevOps.drawio
└── src/main/
    ├── java/br/com/fiap/ClyvoCareAPI/
    │   ├── ClyvoCareApiApplication.java
    │   ├── auth/
    │   │   ├── SecurityConfig.java
    │   │   ├── AuthService.java
    │   │   ├── TokenService.java
    │   │   └── AuthController.java
    │   ├── config/
    │   │   └── OpenApiConfig.java
    │   ├── controller/
    │   │   └── (um por entidade)
    │   ├── service/
    │   │   └── (um por entidade)
    │   ├── repository/
    │   │   └── (um por entidade)
    │   ├── entity/
    │   │   └── (entidades JPA)
    │   ├── dto/
    │   │   └── (XxxRequest + XxxResponse records)
    │   └── validation/
    │       └── ValidationHandler.java
    └── resources/
        ├── application.properties
        ├── db/migration/
        │   ├── V1__create_baseline_schema.sql
        │   └── V2__subscription_enums.sql
        └── keys/
            └── (private_key.pem / public_key.pem — gerados localmente, não versionados)
```

---

## Schema do banco

O schema é versionado por **Flyway**. A V1 histórica cria 15 tabelas e permanece inalterada para preservar os checksums já registrados. A `V2__subscription_enums.sql` converte `STATUS_ID` e `PAYMENT_METHOD_ID` em textos e remove as duas tabelas auxiliares, deixando **13 tabelas de aplicação**. O Hibernate usa `ddl-auto=validate`.

- **Banco existente no modelo antigo:** a V2 preserva IDs, datas e valores das contratações. A conversão usa nomes dos cadastros antigos, sem fixar seus IDs.
- **Banco vazio:** o Flyway aplica V1 e V2. Os dados de demonstração não fazem parte das migrations.
- **Schema criado pelo `docs/script.sql` atual:** o Flyway registra baseline 1; a V2 reconhece as colunas textuais existentes e atualiza o PL/SQL.
- **Schema legado anterior à V1:** a V2 também adiciona `ROLE_NAME`/`ENABLED` em tutores e a tabela de auditoria, se ausentes. Isso não substitui a conferência das demais tabelas antes de registrar o baseline.
- Funções, relatórios e trigger são recompilados e verificados em `USER_ERRORS`. Se existir `SP_INSERT_SUBSCRIPTION`, seus parâmetros de status e pagamento passam a texto.

| Dado antigo | Valor após a V2 |
|---|---|
| `ACTIVE`, `TRIAL` | `ACTIVE` |
| `CANCELED`, `SUSPENDED`, `INACTIVE` | `INACTIVE` |
| `OVERDUE`, `PENDING` | `PENDING` |
| `Card`, `CREDIT CARD`, `CREDIT_CARD` | `CREDIT_CARD` |
| `Auto Debit`, `DEBIT CARD`, `DEBIT_CARD` | `DEBIT_CARD` |
| `Boleto`, `Pix` | `BOLETO`, `PIX` |

Valores já normalizados são mantidos. Contratações que usem outros nomes interrompem a V2 antes da conversão; cadastros antigos sem uso desaparecem com as tabelas auxiliares.

O `docs/script.sql` é o script completo de **recriação** da demonstração: DDL de 13 tabelas, seeds e PL/SQL. Ele contém `DROP TABLE`; para atualizar um banco com dados a preservar, use o Flyway. O `docs/fix.sql` do .NET também foi ajustado aos dois enums; segue como referência histórica sem a tabela de auditoria.

```sql
SELECT FN_CALCULATE_CONTRACT_VALUE(1, 'DEBIT_CARD') FROM DUAL;

BEGIN
    SP_REPORT_SUBSCRIPTIONS_JSON('ACTIVE');
    SP_REPORT_REVENUE_FACT;
END;
/
```

---

## Requisitos da disciplina cobertos

### Java Advanced
- CRUD completo das 8 entidades com retornos HTTP corretos (200, 201, 204, 400, 404, 409)
- Bean Validation com extensões brasileiras (`@CPF`, `@Email`, `@Size`, `@Positive`, `@PastOrPresent`)
- Paginação e ordenação via `Pageable` em Owner, Pet e Subscription
- Busca por parâmetros opcionais combinados
- Pelo menos 3 consultas JPQL personalizadas
- Cache configurado em listagens estáveis
- Tratamento global de exceções com formato JSON consistente
- Documentação Swagger/OpenAPI completa

### DevOps Tools & Cloud Computing
- Script Azure CLI completo (`azure-setup.sh`) provisionando VM, portas, Docker e ferramentas
- Dockerfile com multi-stage build e usuário sem privilégios root
- Docker Compose com dois containers (API + Oracle XE) e volume nomeado para persistência
- Aplicação rodando em background (`docker compose up -d`)
- Volume nomeado `java-sprint-1_oracle-data` garantindo persistência dos dados
- Arquitetura macro documentada em `docs/Arquitetura_DevOps.drawio`

### Java Advanced — Sprint 3
- **Flyway**: V1 histórica + V2 dos enums de status e pagamento; schema atual com 13 tabelas, conversão dos dados existentes e `ddl-auto=validate`
- **Spring Security**: autenticação JWT stateless (RSA/RS256), 2 perfis (`ADMIN`/`OWNER`) via `TB_CAD_OWNER.ROLE_NAME`, rotas protegidas por perfil com `@PreAuthorize`
- 2 fluxos não-CRUD com regra de negócio (contratação de plano com desconto por forma de pagamento; ciclo de vida da assinatura) — em desenvolvimento

---

## Limitações conhecidas

- **docker-compose.yml não versionado.** Por segurança, o arquivo está no `.gitignore`. O script `azure-setup.sh` o cria automaticamente na VM durante o provisionamento.
- **Validação de FK entre APIs não acontece em tempo real.** O Oracle resolve via constraints, mas a UX em casos de borda não é polida.
- **CORS ainda não configurado.** Pendente da URL do deploy do frontend (repositório separado).
- **Sem ownership-scoping.** Um `OWNER` autenticado lista/consulta todos os pets e contratações, não só os seus — as regras de perfil (`ADMIN`/`OWNER`) valem por rota, não por dono do recurso.
- **Java 17 no `pom.xml`, Java 23 no `Dockerfile`.** Inconsistência herdada da conteinerização; a build local usa 17, a imagem Docker usa 23. Decisão de qual usar ainda em aberto.

---
