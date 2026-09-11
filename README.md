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

## Arquitetura Macro na Nuvem (ACR + ACI — mexicocentral)

```
Usuário / Browser / Insomnia
            |
       HTTP :8080
            |
   Microsoft Azure — Região mexicocentral (Resource Group: rg-clyvocare-sprint3)
   ┌────────────────────────────────────────────────────────────────────────┐
   │  Azure Container Registry (ACR): acrclyvoXXXXX (SKU Basic)             │
   │  - Armazena a imagem Docker: clyvocare-api:v1                          │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │ pull image
   ┌───────────────────────────────────▼────────────────────────────────────┐
   │  Azure Container Instances (ACI) — Container Group                     │
   │  FQDN: http://clyvocare-api-XXXXX.mexicocentral.azurecontainer.io:8080 │
   │                                                                        │
   │  ┌─────────────────────────────┐        ┌────────────────────────────┐ │
   │  │ clyvocare-api               │        │ oracle-db                  │ │
   │  │ Spring Boot (Java 23)       │  JDBC  │ Oracle XE 21-slim          │ │
   │  │ Port: 8080 (Public)         ├───────►│ Port: 1521 (Internal)      │ │
   │  │ Non-Root User (UID 10001)   │        │ Database: XEPDB1           │ │
   │  │ CPU: 1.0 | RAM: 1.5 GB      │        │ CPU: 1.0 | RAM: 2.0 GB     │ │
   │  └─────────────────────────────┘        └────────────────────────────┘ │
   └────────────────────────────────────────────────────────────────────────┘
```

---

## Como executar localmente

Pré-requisitos no ambiente:

- Java 17+ instalado e disponível no `PATH`
- Acesso à VPN da FIAP (para conectar no `oracle.fiap.com.br`) ou um Oracle próprio via `SPRING_DATASOURCE_URL`
- Contra a FIAP o Flyway registra o baseline sobre o schema existente; contra um banco vazio o Flyway aplica V1 + V2 e o `DataInitializer` carrega os dados de exemplo (ver [Schema do banco](#schema-do-banco))

O par de chaves RSA que assina o JWT já está versionado em `src/main/resources/keys/` (chaves de demonstração). Para um deploy real, gere um par novo (precisa de OpenSSL):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/keys/private_key.pem
openssl rsa -pubout -in src/main/resources/keys/private_key.pem -out src/main/resources/keys/public_key.pem
```

Rodando a aplicação:

```bash
./mvnw spring-boot:run
```

Por padrão a API sobe na porta `8080`. Se você precisar de outra porta, sobrescreva com `--server.port=XXXX` ou edite o `application.properties`.

---

## Como executar na nuvem (Azure ACR + ACI) — Sprint 3

> **Requisitos DevOps Cumpridos**:
> 1. Solução containerizada completa: **App e Banco de Dados em Containers**.
> 2. 100% dos recursos criados via **Azure CLI**.
> 3. Região: **`mexicocentral`** (Otimizado para **Assinatura de Estudantes**).
> 4. Container da aplicação executando como **usuário não-root / não-admin** (`UID 10001`).
> 5. DDL com estrutura e comentários entregue no arquivo `script_bd.sql`.

### Pré-requisitos

- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) instalado
- Assinatura Azure ativa (Azure for Students)
- Docker instalado (opcional, pois o script utiliza o `az acr build` que compila diretamente na nuvem)

### 1. Login na Azure

```bash
az login
az account set --subscription "NOME_OU_ID_DA_SUA_ASSINATURA"
```

### 2. Executar o script de provisionamento automatizado

O script `azure-acr-aci-setup.sh` na raiz do projeto realiza automaticamente todas as etapas exigidas na Sprint 3:

```bash
chmod +x azure-acr-aci-setup.sh
./azure-acr-aci-setup.sh
```

O script executa em sequência:
1. **Cria o Grupo de Recursos** `rg-clyvocare-sprint3` na região `mexicocentral`.
2. **Cria o Azure Container Registry (ACR)** com SKU `Basic` e habilita autenticação administrativa.
3. **Executa o build da imagem da API** com Dockerfile multi-stage e envia para o ACR (`az acr build`).
4. **Obtém as credenciais de acesso** do ACR.
5. **Gera o manifesto declarativo YAML** para o Azure Container Instances (Container Group).
6. **Provisiona o Container Group no ACI** contendo:
   - **`clyvocare-api`**: Container da API Java (usuário non-root `UID 10001`, porta 8080).
   - **`oracle-db`**: Container do banco de dados Oracle XE 21 (`gvenzl/oracle-xe:21-slim`, porta 1521).
7. **Exibe os endereços públicos**, Swagger UI, endpoint OpenAPI e comandos para verificação de logs.

### 3. Acessar e Testar a Aplicação

O `azure-acr-aci-setup.sh` já aguarda a API responder antes de finalizar (o Oracle XE do container leva de 2 a 4 minutos para inicializar, e a API reinicia até o banco aceitar conexão). Se for rodar `azure-acr-aci-test.sh` ou a collection manualmente, aguarde o script imprimir `API respondendo` ou confirme com `curl http://<FQDN>:8080/v3/api-docs`.

- **Swagger UI**: `http://<FQDN_OU_IP_PUBLICO>:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://<FQDN_OU_IP_PUBLICO>:8080/v3/api-docs`
- **Verificar usuário não-root no container**:
  ```bash
  az container exec --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name clyvocare-api --exec-command "id"
  ```
  *Saída esperada: `uid=10001(appuser) gid=10001(appgroup)`*

- **Visualizar logs em tempo real**:
  ```bash
  # Logs da API Java
  az container logs --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name clyvocare-api --follow

  # Logs do Oracle DB
  az container logs --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name oracle-db --follow
  ```

### 4. Limpeza dos Recursos (Para economizar créditos)

Ao finalizar a gravação do vídeo e validação dos testes, execute o script de limpeza:

```bash
chmod +x azure-cleanup.sh
./azure-cleanup.sh
```

---

## Containerização — Dockerfile (Non-Root User)

O `Dockerfile` na raiz do projeto utiliza multi-stage build e configuração estrita de usuário sem privilégios administrativos:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-23-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# Criando grupo e usuário sem privilégios administrativos (Non-Root)
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
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

O JWT é assinado com **RSA (RS256)**: a API guarda um par de chaves em `src/main/resources/keys/`, o `AuthController` assina com a privada no login, e o `SecurityConfig` valida com a pública em cada request. O par versionado é de demonstração; para produção, gere um par novo e injete por variável de ambiente / secret.

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

Rotas de fluxo de `/contratacoes`:

| Rota | Perfil |
|---|---|
| `POST /contratacoes/simulacao` | ADMIN ou OWNER |
| `PATCH /contratacoes/{id}/status` | ADMIN |
| `POST /contratacoes/{id}/troca-plano` | ADMIN |

`/auth/login` e o Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) são as únicas rotas públicas, além do `POST /responsaveis`.

### CORS

O frontend (React + Vite) roda em outra origem. `CorsConfig` libera `GET/POST/PUT/PATCH/DELETE/OPTIONS` e os headers `Authorization`/`Content-Type` para as origens em `cors.allowed-origins` (default: `http://localhost:5173` e `http://localhost:3000`). Em produção, definir `CORS_ALLOWED_ORIGINS` com a URL do front.

---

## Status e forma de pagamento

Os dois campos usam `@Enumerated(EnumType.STRING)` e colunas `VARCHAR2(20)` na contratação, protegidas por constraints no Oracle:

| Campo JSON / coluna Oracle | Enum Java | Valores |
|---|---|---|
| `status` / `STATUS` | `SubscriptionStatus` | `ACTIVE`, `INACTIVE`, `PENDING` |
| `paymentMethod` / `PAYMENT_METHOD` | `PaymentMethod` | `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`, `PIX` |

`ACTIVE` indica contratação ativa; `PENDING`, pausada ou aguardando regularização; `INACTIVE`, encerrada definitivamente. Uma contratação encerrada não pode ser reativada; o pet pode receber uma nova contratação.

- POST e PUT recebem `petId`, `planId` e `paymentMethod`. O POST define `ACTIVE` no backend; o PUT preserva o status atual. As respostas incluem `status` e `paymentMethod` como strings.
- Filtros: `GET /contratacoes?status=ACTIVE&paymentMethod=PIX`.
- `GET /status-contratacao` e `GET /formas-pagamento` retornam listas fixas, sem consultar o banco. Essas rotas não possuem criação, edição, exclusão ou consulta por ID.
- IDs obrigatórios devem ser positivos. `paymentMethod` ausente, nulo, desconhecido ou numérico retorna HTTP 400; filtros de enum inválidos também retornam 400.

`ContractPricingService` aplica **5% para PIX e 3% para cartão de débito**; crédito e boleto não têm desconto. O cálculo é compartilhado pela simulação, pelo cadastro, pelo PUT e pela troca de plano, com arredondamento monetário para duas casas. A função Oracle `FN_CALCULATE_CONTRACT_VALUE` segue as mesmas taxas.

`POST /contratacoes/simulacao` retorna o preço do plano, a taxa e o valor do desconto e o preço final, sem criar contratação. Na confirmação, o backend consulta novamente o preço do plano e grava o valor calculado.

Um pet pode ter apenas uma contratação `ACTIVE` criada ou ativada por esses fluxos. Cadastro, atualização de contratação ativa e ativação de uma contratação `PENDING` validam duplicidade sob bloqueio do pet dentro de uma transação; conflito retorna HTTP 409. Nas alterações, a consulta exclui o próprio registro. PUT, troca de plano e mudança de status também bloqueiam a contratação durante a operação, para evitar que alterações concorrentes sobrescrevam um encerramento.

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

### Simulação antes de contratar

```http
POST /contratacoes/simulacao
Authorization: Bearer <token>
Content-Type: application/json

{
    "planId": 1,
    "paymentMethod": "PIX"
}
```

Para um plano de R$ 69,90, a resposta HTTP 200 é:

```json
{
    "baseValue": 69.90,
    "discountRate": 0.05,
    "discountAmount": 3.49,
    "finalValue": 66.41
}
```

A simulação e a criação são permitidas para `ADMIN` e `OWNER` autenticados.

### Contratação de um plano

```
POST /contratacoes
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "paymentMethod": "PIX"
}
```

A criação retorna HTTP 201 com status `ACTIVE` e o preço calculado. Se o pet já tiver contratação ativa, retorna HTTP 409.

### Atualizar uma contratação

```
PUT /contratacoes/1
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "paymentMethod": "PIX"
}
```

O PUT exige `ADMIN`, preserva o status e recalcula o preço conforme plano e pagamento informados. Assim como a troca de plano, só aceita contratações `ACTIVE` ou `PENDING`; uma contratação `INACTIVE` retorna HTTP 409.

### Ciclo de vida da contratação

```http
PATCH /contratacoes/1/status
Authorization: Bearer <token>
Content-Type: application/json

{
    "status": "PENDING"
}
```

As transições seguem esta tabela:

| Status atual | Pode mudar para |
|---|---|
| `ACTIVE` | `PENDING`, `INACTIVE` |
| `PENDING` | `ACTIVE`, `INACTIVE` |
| `INACTIVE` | Nenhum — encerramento definitivo |

Uma transição válida retorna HTTP 200 com `SubscriptionResponse`, preservando plano, pagamento, valor e data da contratação. Repetir o status atual, tentar reativar uma contratação encerrada ou ativar uma contratação quando o pet já possui outra ativa retorna HTTP 409. Status ausente, nulo ou inválido retorna 400; contratação inexistente retorna 404.

### Troca de plano

```http
POST /contratacoes/1/troca-plano
Authorization: Bearer <token>
Content-Type: application/json

{
    "planId": 2,
    "paymentMethod": "DEBIT_CARD"
}
```

`planId` é obrigatório e positivo. `paymentMethod` é opcional: omitido ou nulo, mantém o pagamento atual. A operação consulta o preço atual do plano, aplica o desconto e retorna HTTP 200 com `SubscriptionResponse`, preservando pet, status e data da contratação. Só é permitida em `ACTIVE` ou `PENDING`; `INACTIVE` retorna 409. Plano ou contratação inexistente retorna 404; dados inválidos retornam 400.

Os dois novos endpoints exigem JWT pela configuração global existente. As regras específicas por perfil para essas operações ficam para a integração com o responsável por Security.

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

- `clyvo-care-api.yaml` — collection do Insomnia; nas requisições de contratação, preencher `access_token` com o JWT de `/auth/login` e `subscription_id` com o ID retornado pelo cadastro
- `MER.png` — diagrama entidade-relacionamento
- `script.sql` — schema de referência (13 tabelas) + seeds de demonstração + PL/SQL da disciplina de Database
- `Arquitetura_DevOps.drawio` — diagrama de arquitetura na nuvem

A feature de simulação e contratação foi conferida em 07/09/2026 com requisições HTTP reais contra o Oracle FIAP: quatro formas de pagamento, validações 400/404, permissões 401/403, cadastro com desconto, duplicidade 409, PUT com recálculo e duas criações concorrentes (201/409). Foram usados tokens temporários ADMIN/OWNER assinados para essa verificação; o endpoint de login não fez parte dela. Os pets e contratos temporários foram removidos, preservando as 11 contratações originais.

O ciclo de vida e a troca de plano também foram conferidos por HTTP no Oracle FIAP em 07/09/2026: as nove combinações de status, troca em ACTIVE/PENDING com os quatro pagamentos, bloqueio de INACTIVE no PUT e na troca, e concorrência entre ativações, cadastro e ativação, encerramento e troca/PUT. A validação usou tokens temporários, removeu os dados criados e preservou as 11 contratações originais. Não houve alteração nas migrations.

Para conferir o fluxo manualmente, autentique-se como ADMIN para preparar os cadastros e executar o PUT. Use um token OWNER para simular e contratar:

1. Crie um Estado via `POST /estados`
2. Crie uma Cidade via `POST /cidades` referenciando o `stateId`
3. Crie um Owner via `POST /responsaveis` referenciando o `cityId`
4. Crie uma Espécie via `POST /especies`
5. Crie uma Raça via `POST /racas` referenciando o `speciesId`
6. Crie um Pet via `POST /pets` referenciando `ownerId`, `speciesId` e `breedId`
7. Crie um Plano via `POST /planos`
8. Consulte os valores em `GET /formas-pagamento`
9. Consulte os valores em `GET /status-contratacao`
10. Simule o plano via `POST /contratacoes/simulacao` com `planId` e `"paymentMethod": "PIX"`
11. Crie uma Contratação via `POST /contratacoes` com `petId`, `planId` e `"paymentMethod": "PIX"`; confira `ACTIVE` e o preço calculado
12. Repita a contratação para o mesmo pet e confira HTTP 409
13. Como ADMIN, atualize a contratação por PUT usando `"paymentMethod": "DEBIT_CARD"` e confira o desconto de 3% e o status preservado
14. Liste via `GET /contratacoes?status=ACTIVE` e verifique a persistência
15. Altere para `PENDING` via `PATCH /contratacoes/{id}/status` e confira que o valor foi preservado
16. Troque o plano via `POST /contratacoes/{id}/troca-plano`; omita `paymentMethod` para manter o pagamento atual
17. Reative para `ACTIVE`, encerre em `INACTIVE` e confira HTTP 409 ao tentar reativar, trocar plano ou executar PUT na contratação encerrada

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
            └── (private_key.pem / public_key.pem — par de demonstração que assina o JWT)
```

---

## Schema do banco

O schema é versionado por **Flyway**. A V1 histórica cria 15 tabelas e permanece inalterada para preservar os checksums já registrados. A `V2__subscription_enums.sql` converte `STATUS_ID` e `PAYMENT_METHOD_ID` em textos e remove as duas tabelas auxiliares, deixando **13 tabelas de aplicação**. O Hibernate usa `ddl-auto=validate`.

> O versionamento por Flyway é exercido contra o Oracle da FIAP (o esquema já existe: baseline 1, V2 aplicada). O deploy ACI sobe um Oracle XE efêmero acessado como `system` — cenário em que o baseline do Flyway não roda o V1. Por isso o container sobrescreve, apenas nesse ambiente, `SPRING_FLYWAY_ENABLED=false` e `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, deixando o Hibernate criar o schema. O `application.properties` versionado permanece com Flyway ligado e `validate`.

### Dados de demonstração

As migrations Flyway só criam o schema — não inserem dados. A carga de exemplo tem uma única fonte: a classe `config/DataInitializer`, que roda no boot e, se as tabelas estiverem vazias, insere estados, cidades, espécies, raças, planos e dois usuários (`ana@email.com` / ADMIN e `carlos@email.com` / OWNER, senha `senha123`). É controlada por `app.seed.enabled` (default `true`); definir `false` desliga a carga. Contra o Oracle da FIAP, que já tem dados, ela não faz nada.

O `docs/script.sql` é um artefato separado, da disciplina de Database: recriação manual completa (DDL + seeds + PL/SQL), com `DROP TABLE`. Não é executado pela aplicação.

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
- **Fluxo de simulação e contratação:** implementado, com desconto por pagamento, status inicial definido pelo backend, validação de duplicidade e recálculo no PUT.
- **Ciclo de vida da assinatura:** transições entre ACTIVE/PENDING/INACTIVE e troca de plano implementadas, com bloqueio de encerradas e validação de contratação ativa duplicada na ativação.
- **Proteção por perfil dos endpoints de fluxo:** `POST /contratacoes/simulacao` liberado para ADMIN e OWNER; `PATCH /contratacoes/{id}/status` e `POST /contratacoes/{id}/troca-plano` restritos a ADMIN via `@PreAuthorize`.

---

## Limitações conhecidas

- **docker-compose.yml não versionado.** Por segurança, o arquivo está no `.gitignore`. O script `azure-setup.sh` o cria automaticamente na VM durante o provisionamento.
- **Validação de FK entre APIs não acontece em tempo real.** O Oracle resolve via constraints, mas a UX em casos de borda não é polida.
- **Sem ownership-scoping.** Um `OWNER` autenticado lista/consulta todos os pets e contratações, não só os seus — as regras de perfil (`ADMIN`/`OWNER`) valem por rota, não por dono do recurso.
- **Java 17 no `pom.xml`, Java 23 no `Dockerfile`.** Inconsistência herdada da conteinerização; a build local usa 17, a imagem Docker usa 23. Decisão de qual usar ainda em aberto.

---
