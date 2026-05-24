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
- `TB_CAD_PAYMENT_METHOD`, `TB_CAD_SUB_STATUS` — lookups de domínio

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
| Segurança de senha | jBCrypt (BCrypt standalone)                                   |
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

- Java 23 instalado e disponível no `PATH`
- Acesso à VPN da FIAP (necessário para alcançar `oracle.fiap.com.br`)
- Credenciais do banco Oracle FIAP (RM e senha)
- Schema da FIAP populado executando o arquivo `docs/fix.sql` no SQL Developer logado com seu RM

Configuração de credenciais. Abra `src/main/resources/application.properties` e preencha:

```properties
spring.datasource.username=SEU_RM_AQUI
spring.datasource.password=SUA_SENHA_AQUI
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

**Senhas com BCrypt.** Toda senha de tutor passa pelo `BCrypt.hashpw` antes de ser salva. O campo `passwordHash` da entidade tem `@JsonIgnore`, então mesmo se alguém esquecer de filtrar no DTO, a senha nunca vaza no JSON de resposta.

**Cache nas listagens estáveis.** Estados e Cidades são marcados com `@Cacheable`. Quando alguém faz POST/PUT/DELETE nesses recursos, `@CacheEvict` limpa tudo. Para entidades de negócio (Owner, Pet, Subscription) não usamos cache porque o volume de mudanças é maior e o risco de servir dado stale supera o benefício.

**Validação de FK no Service, não no banco.** Antes de inserir uma Cidade, por exemplo, o `CityService` consulta o `StateService` para garantir que o estado informado existe. Se não existe, lança 404 com mensagem clara em vez de deixar o Oracle gritar `ORA-02291`.

**Tratamento global de erros.** A classe `ValidationHandler` (anotada com `@RestControllerAdvice`) captura dois tipos de exceção e devolve JSON consistente:

- `MethodArgumentNotValidException` (erros de Bean Validation) → 400 com lista `[{field, message}]`
- `ResponseStatusException` (404s do `findById`, 409s de conflito) → 400/404/409 com `{status, message}`

**Paginação onde faz sentido.** Lookups pequenos (Estados, Espécies, Planos) devolvem `List<T>` direto. Já as entidades de negócio (Owner, Pet, Subscription) usam `Pageable` no GET, com filtros opcionais via query params.

---

## Endpoints principais

Documentação completa interativa está no Swagger UI em `http://localhost:8080/swagger-ui.html`.

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
    "statusId": 1,
    "paymentMethodId": 1
}
```

### Trocar status de uma contratação

```
PUT /contratacoes/1
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "statusId": 2,
    "paymentMethodId": 1
}
```

### Filtros úteis em contratações

```
GET /contratacoes?statusId=1        # todas ativas
GET /contratacoes?petId=1           # histórico de contratações de um pet
GET /contratacoes?planId=5&statusId=2  # cancelamentos do plano Total
```

### Lookups (Estados, Cidades, Espécies, etc)

- `GET /estados`
- `GET /cidades`
- `GET /especies`
- `GET /racas`
- `GET /planos`
- `GET /formas-pagamento`
- `GET /status-contratacao`

CRUD completo (POST, PUT, DELETE) também existe nessas rotas para operações administrativas.

---

## Testando localmente

A pasta `docs/` contém:

- `clyvo-care-api.yaml` — collection completa do Insomnia
- `MER.png` — diagrama entidade-relacionamento
- `fix.sql` — script completo de criação do schema Oracle
- `Arquitetura_DevOps.drawio` — diagrama de arquitetura na nuvem

Para um teste end-to-end rápido:

1. Crie um Estado via `POST /estados`
2. Crie uma Cidade via `POST /cidades` referenciando o `stateId`
3. Crie um Owner via `POST /responsaveis` referenciando o `cityId`
4. Crie uma Espécie via `POST /especies`
5. Crie uma Raça via `POST /racas` referenciando o `speciesId`
6. Crie um Pet via `POST /pets` referenciando `ownerId`, `speciesId` e `breedId`
7. Crie um Plano via `POST /planos`
8. Crie uma Forma de Pagamento via `POST /formas-pagamento`
9. Crie um Status via `POST /status-contratacao`
10. Crie uma Contratação via `POST /contratacoes`
11. Liste via `GET /contratacoes` e verifique a persistência

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
│   ├── fix.sql
│   ├── MER.png
│   └── Arquitetura_DevOps.drawio
└── src/main/java/br/com/fiap/ClyvoCareAPI/
    ├── ClyvoCareApiApplication.java
    ├── config/
    │   └── OpenApiConfig.java
    ├── controller/
    │   └── (um por entidade)
    ├── service/
    │   └── (um por entidade)
    ├── repository/
    │   └── (um por entidade)
    ├── entity/
    │   └── (entidades JPA)
    ├── dto/
    │   └── (XxxRequest + XxxResponse records)
    └── validation/
        └── ValidationHandler.java
```

---

## Schema do banco

O banco é criado automaticamente pelo Hibernate na primeira inicialização (`ddl-auto=update`). O arquivo `docs/fix.sql` contém o schema completo para uso com o Oracle FIAP:

- DDL das 14 tabelas (cadastro + saúde + log de erros)
- 13 stored procedures de inserção com tratamento de exceção
- Carga de dados de exemplo (8 estados, 12 cidades, 4 espécies, 12 raças, 5 planos, etc)
- Blocos de análise PL/SQL (joins, LAG/LEAD, cursores explícitos com decisão)

---

## Requisitos da disciplina cobertos

### Java Advanced
- CRUD completo das 10 entidades com retornos HTTP corretos (200, 201, 204, 400, 404, 409)
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

---

## Limitações conhecidas

- **Sem autenticação.** A API não tem login implementado. O `passwordHash` é guardado corretamente, mas não há endpoint `/login` nem token JWT. Decisão consciente: escopo da disciplina foca em REST/JPA.
- **docker-compose.yml não versionado.** Por segurança, o arquivo está no `.gitignore`. O script `azure-setup.sh` o cria automaticamente na VM durante o provisionamento.
- **Validação de FK entre APIs não acontece em tempo real.** O Oracle resolve via constraints, mas a UX em casos de borda não é polida.
- **Migrations não automatizadas.** O schema é criado via `ddl-auto=update`. Em produção, usaríamos Flyway ou Liquibase.

---
