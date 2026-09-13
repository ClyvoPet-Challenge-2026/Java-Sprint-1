# 🐾 ClyvoCare API — Cadastros e Contratação

API Java do projeto **ClyvoCare**, desenvolvida como parte do Challenge FIAP 2026 (2TDSPG, Java Advanced). É a metade administrativa de um sistema de plano de saúde para pets inspirado em serviços como o Petlove Saúde, cuidando do cadastro de tutores, pets, planos disponíveis e das contratações desses planos.

O projeto tem uma irmã em C# (.NET Advanced) que cuida da operação clínica em si (clínicas, eventos clínicos, lembretes). As duas APIs compartilham o mesmo banco Oracle FIAP via um schema que serve como contrato entre elas, cada uma sendo dona da escrita das suas próprias tabelas e apenas lendo as do outro contexto quando precisa de informações para exibir.

---

## Índice

- [Sobre a divisão por domínio](#sobre-a-divisão-por-domínio)
- [Frontend web (camada de visualização)](#frontend-web-camada-de-visualização)
- [Stack técnica](#stack-técnica)
- [Benefícios para o Negócio](#benefícios-para-o-negócio)
- [Arquitetura macro na nuvem](#arquitetura-macro-na-nuvem)
- [Como executar localmente](#como-executar-localmente)
- [Como executar na nuvem (Azure ACR + ACI)](#como-executar-na-nuvem-azure-acr--aci)
- [Containerização — Dockerfile](#containerização--dockerfile)
- [Arquitetura da aplicação](#arquitetura-da-aplicação)
- [Autenticação e Autorização](#autenticação-e-autorização)
- [Status e forma de pagamento](#status-e-forma-de-pagamento)
- [Endpoints principais](#endpoints-principais)
- [Testando a API](#testando-a-api)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Banco de dados](#banco-de-dados)
- [Requisitos da disciplina cobertos](#requisitos-da-disciplina-cobertos)

---

## Sobre a divisão por domínio

A escolha foi separar as duas APIs por **bounded context** (contexto delimitado), e não por questões técnicas como performance. A ideia é simples: uma API cuida de quem somos (cadastros estáveis), a outra cuida do que fazemos pelo pet (jornada de cuidado). Essa fronteira fica explícita no nome dos pacotes, na estrutura do banco e nos pacotes que cada lado pode escrever.

Esta API (Java) é dona de escrita das seguintes tabelas:

- `TB_CAD_OWNER` — tutores dos pets
- `TB_CAD_PET` — os pets em si
- `TB_CAD_PLAN` — catálogo de planos (Essential, Basic, Premium, Master, Total, Corporate)
- `TB_CAD_SUBSCRIPTION` — contratações de plano feitas pelos tutores
- `TB_CAD_SPECIES`, `TB_CAD_BREED` — taxonomia de espécies e raças
- `TB_CAD_STATE`, `TB_CAD_CITY` — localização

Status e forma de pagamento são enums armazenados nas colunas `STATUS` e `PAYMENT_METHOD` de `TB_CAD_SUBSCRIPTION`, sem tabelas auxiliares.

A API C# escreve em `TB_CAD_CLINIC`, `TB_HEA_CLINICAL_EVENT` e `TB_HEA_REMINDER`, e lê algumas das tabelas acima quando precisa.

---

## Frontend web (camada de visualização)

Este repositório é 100% backend — API REST pura, sem nenhuma tela. A camada de visualização exigida pela disciplina (Java Advanced, Sprint 3) vive em um repositório próprio, dedicado só ao frontend:

🔗 **[ClyvoCare Web](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web)**

É uma SPA em **React + Vite** que consome esta API via HTTP e cobre, na prática, o que o backend já modela:

- **Login com JWT** (`POST /auth/login`) e sessão persistida no navegador, sem repetir autenticação a cada acesso
- **Dois perfis de usuário com telas diferentes** (`ADMIN` e `OWNER`), refletindo no frontend a mesma proteção de rotas por perfil que já existe aqui no backend (ver [Autenticação e Autorização](#autenticação-e-autorização))
- **CRUD completo de Pets**, com formulário de cadastro, edição e remoção
- **Fluxo de contratação de plano** (simula o preço com desconto antes de confirmar) e **fluxo de gestão de contratações** (troca de status), os dois fluxos de negócio não-CRUD descritos em [Status e forma de pagamento](#status-e-forma-de-pagamento)

### Rodando os dois juntos

1. Suba esta API — local (`./mvnw spring-boot:run`) ou aponte para o FQDN do deploy em nuvem (ver [Como executar na nuvem](#como-executar-na-nuvem-azure-acr--aci))
2. Clone o [repositório do frontend](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web)
3. Configure `VITE_API_BASE_URL` no `.env` (aponte para `http://localhost:8080` ou para o FQDN do ACI)
4. Rode `npm install && npm run dev` e acesse `http://localhost:5173`

O CORS desta API já libera `http://localhost:5173` por padrão (ver [CORS](#cors)), então nenhum ajuste adicional é necessário para o desenvolvimento local do front.

---

## Stack técnica

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Persistência | Spring Data JPA + Hibernate 7 |
| Banco de dados | Oracle 19c (FIAP) / Oracle XE 21 (deploy em nuvem) |
| Driver JDBC | ojdbc11 |
| Migrations | Flyway |
| Validação | Bean Validation (Hibernate Validator + extensões BR para CPF) |
| Cache | Spring Cache (in-memory) |
| Autenticação | JWT stateless assinado com RSA (RS256), via Spring Security |
| Segurança de senha | Spring Security (`BCryptPasswordEncoder`) |
| Documentação | SpringDoc OpenAPI (Swagger UI) |
| Boilerplate | Lombok |
| Build | Maven |
| Containerização | Docker (multi-stage build, usuário non-root) |
| Nuvem | Microsoft Azure — Container Registry + Container Instances (ACR + ACI) |

---

## Benefícios para o Negócio

O ClyvoCare resolve um problema real do mercado pet brasileiro: a fragmentação e informalidade no acompanhamento de saúde dos animais. Ao digitalizar o cadastro de tutores, pets, planos e contratações em uma API robusta e escalável, o sistema oferece:

- **Gestão centralizada** de toda a base de clientes e seus pets em um único sistema
- **Rastreabilidade completa** do histórico de contratações por pet, permitindo análises de churn e upsell
- **Flexibilidade de planos** com 6 tiers (Essential a Corporate), cobrindo diferentes perfis de tutor e pet
- **Segurança de dados** com senhas hasheadas em BCrypt e validação rigorosa de CPF e e-mail
- **Escalabilidade na nuvem** via containerização Docker no Azure, permitindo crescimento sem reconfiguração de infraestrutura
- **Integração nativa** com a API clínica (.NET), formando um ecossistema completo de saúde pet

---

## Arquitetura macro na nuvem

```
Usuário / Browser / Insomnia
            |
       HTTP :8080
            |
   Microsoft Azure — Região mexicocentral (Resource Group: rg-clyvocare-sprint3)
   ┌────────────────────────────────────────────────────────────────────────┐
   │  Azure Container Registry (ACR) — SKU Basic                            │
   │  Armazena a imagem Docker: clyvocare-api:v1                            │
   └───────────────────────────────────┬────────────────────────────────────┘
                                        │ pull image
   ┌───────────────────────────────────▼────────────────────────────────────┐
   │  Azure Container Instances (ACI) — Container Group                     │
   │  FQDN: http://<label>.mexicocentral.azurecontainer.io:8080             │
   │                                                                        │
   │  ┌─────────────────────────────┐        ┌────────────────────────────┐ │
   │  │ clyvocare-api               │        │ oracle-db                  │ │
   │  │ Spring Boot                 │  JDBC  │ Oracle XE 21-slim          │ │
   │  │ Porta: 8080 (pública)       ├───────►│ Porta: 1521 (interna)      │ │
   │  │ Usuário non-root (UID 10001)│        │ Database: XEPDB1           │ │
   │  │ CPU: 1.0 | RAM: 1.5 GB      │        │ CPU: 1.0 | RAM: 2.5 GB     │ │
   │  └─────────────────────────────┘        └────────────────────────────┘ │
   └────────────────────────────────────────────────────────────────────────┘
```

> Requisitos de DevOps cumpridos por este deploy: solução 100% containerizada (API e banco em containers separados no mesmo Container Group); todos os recursos provisionados via Azure CLI; região `mexicocentral` (compatível com assinaturas Azure for Students); container da aplicação rodando como usuário non-root (`UID 10001`); DDL com estrutura e comentários entregue em `script_bd.sql`.

---

## Como executar localmente

Pré-requisitos no ambiente:

- Java 17+ instalado e disponível no `PATH`
- Acesso à VPN da FIAP (para conectar no `oracle.fiap.com.br`) ou um Oracle próprio via `SPRING_DATASOURCE_URL`
- Contra a FIAP o Flyway registra o baseline sobre o schema existente quando ainda não há histórico e aplica as migrations posteriores; contra um banco vazio aplica V1 + V2 + V3. A V3 carrega os dados de exemplo e corrige os placeholders de senha dos dois usuários de demonstração (ver [Banco de dados](#banco-de-dados)).

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

## Como executar na nuvem (Azure ACR + ACI)

### Pré-requisitos

- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) instalado
- Assinatura Azure ativa (Azure for Students)
- Docker Desktop instalado e em execução — o script builda a imagem localmente e envia pro ACR via `docker push`

### 1. Login na Azure

```bash
az login
az account set --subscription "NOME_OU_ID_DA_SUA_ASSINATURA"
```

### 2. Executar o script de provisionamento automatizado

O script `azure-acr-aci-setup.sh`, na raiz do projeto, roda de dentro de um terminal já aberto (não por duplo-clique — veja o aviso abaixo):

```bash
chmod +x azure-acr-aci-setup.sh
./azure-acr-aci-setup.sh
```

Ele executa, em sequência:

1. Verifica a autenticação no Azure CLI
2. Registra os resource providers `Microsoft.ContainerRegistry` e `Microsoft.ContainerInstance` (idempotente — necessário em assinaturas novas)
3. Cria o Resource Group `rg-clyvocare-sprint3`
4. Cria o Azure Container Registry (SKU Basic)
5. Obtém as credenciais do ACR e autentica o Docker local
6. Builda a imagem da API (multi-stage) e envia para o ACR
7. Gera o manifesto declarativo do Container Group (API + Oracle XE)
8. Provisiona o Container Group no ACI
9. Obtém IP e FQDN públicos
10. Aguarda a API responder em `/v3/api-docs` (o Oracle XE leva de 2 a 4 minutos para inicializar) antes de imprimir o resumo final

> ⚠️ **Não rode o script com duplo-clique.** Isso abre uma janela temporária que fecha sozinha assim que ele termina, levando a saída (com a URL do deploy) junto. Abra um terminal (Git Bash) e rode o comando de dentro dele.

### 3. Onde pegar a URL pra testar

O FQDN muda a cada execução do script. Isso não é um problema: o deploy fica salvo no Azure, então **qualquer terminal, a qualquer momento depois**, com acesso à assinatura (`az login`), recupera a URL vigente:

```bash
az container show --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --query ipAddress.fqdn -o tsv
```

Confirma que está no ar:

```bash
az container show --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --query instanceView.state -o tsv
```
(deve responder `Running`)

Com o FQDN em mãos (`http://<fqdn>:8080`):

- **Swagger UI**: `http://<fqdn>:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://<fqdn>:8080/v3/api-docs`
- **Collection Insomnia**: importe `docs/clyvo-care-api.yaml`, selecione o environment **Azure ACI** e cole o FQDN em `base_url` (ver [Testando a API](#testando-a-api))

Outros comandos úteis:

```bash
# Confirma que o container roda sem privilégios de root
az container exec --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name clyvocare-api --exec-command "id"
# Saída esperada: uid=10001(appuser) gid=10001(appgroup)

# Logs em tempo real
az container logs --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name clyvocare-api --follow
az container logs --resource-group rg-clyvocare-sprint3 --name aci-clyvocare-group --container-name oracle-db --follow
```

### 4. Limpeza dos recursos (para economizar créditos)

Ao finalizar a validação e a gravação do vídeo, execute o script de limpeza:

```bash
chmod +x azure-cleanup.sh
./azure-cleanup.sh
```

---

## Containerização — Dockerfile

O `Dockerfile` na raiz do projeto usa multi-stage build e roda com um usuário sem privilégios administrativos:

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
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
```

> A API roda com o usuário `appuser`, sem privilégios de root, atendendo ao requisito de segurança de contêiner da disciplina de DevOps.

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

`GET /auth/me` (qualquer perfil autenticado) devolve o responsável dono do token.

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

`/auth/login`, `/auth/me` (autenticado) e o Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) são as únicas rotas fora do padrão acima, além do `POST /responsaveis` (público).

### Usuários de teste (seed de demonstração)

| Email | Senha | Perfil |
|---|---|---|
| `ana@email.com` | `senha123` | ADMIN |
| `carlos@email.com` | `senha123` | OWNER |

### CORS

O [frontend](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web) (React + Vite, ver [Frontend web](#frontend-web-camada-de-visualização)) roda em outra origem. `CorsConfig` libera `GET/POST/PUT/PATCH/DELETE/OPTIONS` e os headers `Authorization`/`Content-Type` para as origens em `cors.allowed-origins` (default: `http://localhost:5173` e `http://localhost:3000`). Em produção, definir `CORS_ALLOWED_ORIGINS` com a URL do front.

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

---

## Endpoints principais

Documentação completa interativa está no Swagger UI em `http://localhost:8080/swagger-ui.html` (ou `http://<fqdn>:8080/swagger-ui.html` no deploy). Exceto `POST /auth/login` e `POST /responsaveis`, todo endpoint abaixo exige `Authorization: Bearer <token>` (ver [Autenticação e Autorização](#autenticação-e-autorização)).

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

Uma transição válida retorna HTTP 200 com `SubscriptionResponse`, preservando plano, pagamento, valor e data da contratação. Repetir o status atual, tentar reativar uma contratação encerrada ou ativar uma contratação quando o pet já possui outra ativa retorna HTTP 409. Status ausente, nulo ou inválido retorna 400; contratação inexistente retorna 404. Essa rota, junto com a troca de plano, é restrita a `ADMIN` (ver [Autenticação e Autorização](#autenticação-e-autorização)).

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

### Filtros úteis em contratações

```
GET /contratacoes?status=ACTIVE             # todas ativas
GET /contratacoes/minhas?page=0&size=6       # planos ativos dos pets do usuário do JWT
GET /contratacoes?petId=1                   # histórico de contratações de um pet
GET /contratacoes?planId=5&status=INACTIVE  # contratações inativas do plano Total
```

### Lookups (Estados, Cidades, Espécies, etc.)

- `GET /estados`
- `GET /cidades`
- `GET /especies`
- `GET /racas`
- `GET /planos`
- `GET /formas-pagamento` — lista fixa dos valores do enum; sem CRUD
- `GET /status-contratacao` — lista fixa dos valores do enum; sem CRUD

CRUD completo (POST, PUT, DELETE) existe nos lookups persistidos para operações administrativas. Os status e as formas de pagamento são fixos e possuem apenas consulta.

---

## Testando a API

### Swagger UI

`http://localhost:8080/swagger-ui.html` (ou `http://<fqdn>:8080/swagger-ui.html` no deploy) mostra o contrato completo da API. Ele **não tem um botão "Authorize"** — decisão consciente do time, para seguir o mesmo padrão do exemplo do professor, que também não configura esse recurso. Ou seja, pelo Swagger dá pra ver a documentação, testar `POST /auth/login` e `POST /responsaveis` (as duas rotas públicas), e confirmar que as demais respondem `401` sem token — mas não dá pra passar o `Authorization` numa rota protegida.

### Collection Insomnia

`docs/clyvo-care-api.yaml` é a collection completa, com dois environments:

- **Base Environment** — `base_url = http://localhost:8080`, para testar local
- **Azure ACI** — troque `base_url` pelo FQDN do deploy (ver [Como executar na nuvem](#como-executar-na-nuvem-azure-acr--aci))

Fluxo de uso:

1. Selecione o environment.
2. Rode **`00 - Autenticacao > Login`** — ele guarda o JWT em `access_token` automaticamente (script de after-response). Todas as demais requisições já usam `Bearer {{ _.access_token }}`.
3. Rode as pastas na ordem: **01 (lookups)** → **02 (cadastros ADMIN)** → **03 (responsáveis e pets)** → **04 (Fluxo A — simulação e contratação)** → **05 (Fluxo B — ciclo de vida)**.

As requests de cadastro (Responsável, Pet, Contratação) encadeiam os IDs automaticamente (`owner_id`, `pet_id`, `subscription_id`), então o Fluxo A e o Fluxo B rodam em sequência sem edição manual.

### Sem Insomnia — fallback com `curl`

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@email.com","password":"senha123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

curl -s http://localhost:8080/planos -H "Authorization: Bearer $TOKEN"
```

---

## Estrutura de pastas

```
Java-Sprint-1/
├── Dockerfile                          ← build multi-stage da API
├── azure-acr-aci-setup.sh              ← provisiona ACR + ACI (Sprint 3)
├── azure-acr-aci-test.sh               ← smoke test contra o deploy
├── azure-cleanup.sh                    ← apaga o resource group
├── azure-setup.sh                      ← script legado (VM da Sprint 1)
├── script_bd.sql                       ← DDL + seeds + PL/SQL (disciplina de Database)
├── .gitignore                          ← docker-compose.yml e o manifesto ACI gerado ficam fora do git
├── pom.xml
├── mvnw / mvnw.cmd
├── docs/
│   ├── clyvo-care-api.yaml             ← collection Insomnia
│   ├── script.sql                      ← mesmo conteúdo de script_bd.sql
│   └── MER.png                         ← diagrama entidade-relacionamento
└── src/main/
    ├── java/br/com/fiap/ClyvoCareAPI/
    │   ├── ClyvoCareApiApplication.java
    │   ├── auth/
    │   │   ├── SecurityConfig.java
    │   │   ├── AuthService.java
    │   │   ├── TokenService.java
    │   │   └── AuthController.java
    │   ├── config/
    │   │   ├── OpenApiConfig.java
    │   │   ├── CorsConfig.java
    │   │   └── DataInitializer.java
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
        │   ├── V2__subscription_enums.sql
        │   └── V3__seed_demo_data.sql
        └── keys/
            └── (private_key.pem / public_key.pem — par de demonstração que assina o JWT)
```

---

## Banco de dados

O schema é versionado por **Flyway**. A V1 histórica cria 15 tabelas e permanece inalterada para preservar os checksums já registrados. A `V2__subscription_enums.sql` converte `STATUS_ID` e `PAYMENT_METHOD_ID` em textos e remove as duas tabelas auxiliares, deixando **13 tabelas de aplicação**. O Hibernate usa `ddl-auto=validate`.

> O versionamento por Flyway é exercido contra o Oracle da FIAP (o esquema já existe: baseline 1, V2 aplicada). O deploy ACI sobe um Oracle XE efêmero acessado como `system` — cenário em que o baseline do Flyway não roda o V1. Por isso o container sobrescreve, apenas nesse ambiente, `SPRING_FLYWAY_ENABLED=false` e `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, deixando o Hibernate criar o schema. O `application.properties` versionado permanece com Flyway ligado e `validate`.

### Dados de demonstração

A execução normal usa a migration `V3__seed_demo_data.sql` para inserir estados, cidades, espécies, raças, planos e os dois usuários de demonstração. A carga usa `MERGE` por chaves de negócio, sem IDs fixos: inclui os registros ausentes mesmo em tabelas parcialmente preenchidas e preserva os cadastros e preços existentes.

Para `ana@email.com` e `carlos@email.com`, a V3 substitui somente os placeholders legados `hash1` e `hash2`, respectivamente, por BCrypt da senha `senha123`. Senhas diferentes desses placeholders são preservadas. Os perfis dessas duas contas de demonstração são ajustados para `ADMIN` e `OWNER`; contas novas são habilitadas, e a situação de habilitação das contas existentes é preservada.

O Flyway registra a V3 no histórico e não a repete a cada inicialização. Para um banco já atualizado até V2, basta iniciar a aplicação normalmente com Flyway habilitado para aplicar a carga. V1 e V2 permanecem inalteradas. A V3 não apaga tabelas nem recria registros existentes.

Enquanto o deploy ACI mantiver `SPRING_FLYWAY_ENABLED=false`, a classe `config/DataInitializer` continua como alternativa para esse ambiente, controlada por `app.seed.enabled` (default `true`). Ela só é ativada com Flyway explicitamente desabilitado. `app.seed.enabled=false` desativa essa alternativa Java, mas não impede a execução da migration V3. O ajuste do Azure ACI para executar Flyway está pendente; seus scripts não foram alterados nesta mudança.

`docs/script.sql` (idêntico a `script_bd.sql`, na raiz) é um artefato separado, da disciplina de Database: recriação manual completa (DDL + seeds + PL/SQL), com `DROP TABLE`. Não é executado pela aplicação.

### Migração V1 → V2

- **Banco existente no modelo antigo:** a V2 preserva IDs, datas e valores das contratações. A conversão usa nomes dos cadastros antigos, sem fixar seus IDs.
- **Banco vazio:** o Flyway aplica V1 e V2 para a estrutura, seguidas de V3 para os dados de demonstração.
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

### Java Advanced (Sprint 1)
- CRUD completo das 8 entidades com retornos HTTP corretos (200, 201, 204, 400, 404, 409)
- Bean Validation com extensões brasileiras (`@CPF`, `@Email`, `@Size`, `@Positive`, `@PastOrPresent`)
- Paginação e ordenação via `Pageable` em Owner, Pet e Subscription
- Busca por parâmetros opcionais combinados
- Consultas JPQL personalizadas nos repositórios
- Cache configurado em listagens estáveis
- Tratamento global de exceções com formato JSON consistente
- Documentação Swagger/OpenAPI completa

### Java Advanced (Sprint 3)
- **Frontend:** camada de visualização entregue em repositório próprio ([ClyvoCare Web](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web)), com login JWT, dois perfis de usuário com telas e rotas protegidas, e dois fluxos completos não-CRUD (contratação de plano e gestão de status) — ver [Frontend web](#frontend-web-camada-de-visualização)
- **Flyway:** V1 histórica + V2 dos enums de status e pagamento + V3 da carga de demonstração; schema atual com 13 tabelas, conversão dos dados existentes e `ddl-auto=validate`
- **Spring Security:** autenticação JWT stateless (RSA/RS256), 2 perfis (`ADMIN`/`OWNER`) via `TB_CAD_OWNER.ROLE_NAME`, rotas protegidas por perfil com `@PreAuthorize`
- **Fluxo de simulação e contratação:** desconto por forma de pagamento, status inicial definido pelo backend, validação de duplicidade e recálculo no PUT
- **Ciclo de vida da assinatura:** transições entre `ACTIVE`/`PENDING`/`INACTIVE` e troca de plano, com bloqueio de encerradas e validação de contratação ativa duplicada na ativação
- **Proteção por perfil dos endpoints de fluxo:** `POST /contratacoes/simulacao` liberado para `ADMIN` e `OWNER`; `PATCH /contratacoes/{id}/status` e `POST /contratacoes/{id}/troca-plano` restritos a `ADMIN`

### DevOps Tools & Cloud Computing
- Solução 100% containerizada: API e banco de dados em containers separados, no mesmo Container Group ACI
- Todos os recursos provisionados via Azure CLI (`azure-acr-aci-setup.sh`), sem passos manuais no portal
- Imagem Docker multi-stage, publicada em um Azure Container Registry próprio
- Container da aplicação rodando como usuário non-root (`UID 10001`)
- Região `mexicocentral`, compatível com assinaturas Azure for Students
- DDL com estrutura e comentários entregue em `script_bd.sql`
