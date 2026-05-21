# ClyvoCare API - Cadastros e Contratação

API Java do projeto **ClyvoCare**, desenvolvida como parte do Challenge FIAP 2026 (2TDSPG, Java Advanced). É a metade administrativa de um sistema de plano de saúde para pets inspirado em serviços como o Petlove Saúde, cuidando do cadastro de tutores, pets, planos disponíveis e das contratações desses planos.

O projeto tem uma irmã em C# (.NET Advanced) que cuida da operação clínica em si (clínicas, eventos clínicos, lembretes). As duas APIs compartilham o mesmo banco Oracle FIAP via um schema que serve como contrato entre elas, cada uma sendo dona da escrita das suas próprias tabelas e apenas lendo as do outro contexto quando precisa de informações para exibir.

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

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 23 |
| Framework | Spring Boot 4.0.6 |
| Persistência | Spring Data JPA + Hibernate 7 |
| Banco de dados | Oracle 19c (FIAP) |
| Driver JDBC | ojdbc11 |
| Validação | Bean Validation (Hibernate Validator + extensões BR para CPF) |
| Cache | Spring Cache (in-memory) |
| Segurança de senha | jBCrypt (BCrypt standalone) |
| Documentação | SpringDoc OpenAPI (Swagger UI) |
| Boilerplate | Lombok |
| Build | Maven |

## Como executar

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

Se você ver `jdbc:h2:mem:...` em vez disso, significa que algo está caindo no H2 (fallback). Confira a connection string e as credenciais.

## Arquitetura da aplicação

A API segue uma arquitetura em camadas bem tradicional do mundo Spring, sem muita ginástica arquitetural — o foco é clareza e cobertura dos requisitos da disciplina, não criar abstrações desnecessárias.

```
Controller (REST)  -->  Service (lógica + cache + validação de FK)  -->  Repository (JPA)  -->  Oracle
       |                          |
       v                          v
     DTOs                     Entities
```

A regra prática: o Controller só conhece DTOs (Request e Response), o Service trabalha com entidades JPA e regras de negócio, o Repository fala com o banco. A conversão entre DTO e Entity acontece nas bordas (`request.toEntity(...)` na entrada, `Response.fromEntity(entity)` na saída), seguindo um padrão de records imutáveis.

Decisões que valem destacar:

**Senhas com BCrypt**. Toda senha de tutor passa pelo `BCrypt.hashpw` antes de ser salva. O campo `passwordHash` da entidade tem `@JsonIgnore`, então mesmo se alguém esquecer de filtrar no DTO, a senha nunca vaza no JSON de resposta.

**Cache nas listagens estáveis**. Estados e Cidades, que praticamente nunca mudam mas são consultados em todo formulário de cadastro, são marcados com `@Cacheable`. Quando alguém faz POST/PUT/DELETE nesses recursos, `@CacheEvict` limpa tudo. Para entidades de negócio (Owner, Pet, Subscription) não usamos cache porque o volume de mudanças é maior e o risco de servir dado stale supera o benefício.

**Validação de FK no Service, não no banco**. Antes de inserir uma Cidade, por exemplo, o `CityService` consulta o `StateService` para garantir que o estado informado existe. Se não existe, lança 404 com mensagem clara em vez de deixar o Oracle gritar `ORA-02291: integrity constraint violated`. O banco ainda tem as FKs como rede de segurança, mas o erro amigável vem antes.

**Tratamento global de erros**. A classe `ValidationHandler` (anotada com `@RestControllerAdvice`) captura dois tipos de exceção e devolve JSON consistente:

- `MethodArgumentNotValidException` (erros de Bean Validation) → 400 com lista `[{field, message}]`
- `ResponseStatusException` (404s do `findById`, 409s de conflito) → 400/404/409 com `{status, message}`

**Paginação onde faz sentido**. Lookups pequenos (Estados, Espécies, Planos) devolvem `List<T>` direto, sem paginar — não tem sentido. Já as entidades de negócio (Owner, Pet, Subscription) usam `Pageable` no GET, com filtros opcionais via query params (`?name=`, `?ownerId=` etc).

## Endpoints principais

Documentação completa interativa está no Swagger UI em `http://localhost:8080/swagger-ui.html`. Aqui ficam os endpoints mais relevantes para entender o fluxo.

### Cadastro de um responsável (tutor do pet)

```http
POST /responsaveis
Content-Type: application/json

{
    "name": "Ana Paula Souza",
    "cpf": "111.111.111-11",
    "email": "ana@email.com",
    "password": "minhasenha123",
    "phone": "(11) 91111-1111",
    "cityId": 1
}
```

A senha vem em texto puro no request e é hasheada antes do save. O `cityId` é validado antes — se não existir, retorna 404. Se CPF ou email já estiverem cadastrados, retorna 409. Resposta 201 traz o tutor criado sem a senha:

```json
{
    "id": 1,
    "name": "Ana Paula Souza",
    "cpf": "111.111.111-11",
    "email": "ana@email.com",
    "phone": "(11) 91111-1111",
    "city": { "id": 1, "name": "Sao Paulo", "state": { "id": 1, "name": "Sao Paulo", "uf": "SP" } },
    "createdAt": "2026-05-21T09:59:32"
}
```

### Listar responsáveis com busca e paginação

```http
GET /responsaveis?name=ana&page=0&size=10&sort=name,asc
```

Os filtros `name`, `cpf` e `email` são todos opcionais. Nome e email fazem busca parcial case-insensitive; CPF é exato. A query atrás disso usa JPQL com cláusulas `IS NULL OR` para que filtros vazios sejam ignorados, retornando uma `Page<OwnerResponse>` com `content`, `totalElements`, `totalPages` etc.

### Cadastro de um pet

```http
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

Pet tem três FKs (owner, species, breed), todas validadas no Service. A raça é opcional — útil para vira-latas, basta omitir `breedId` do JSON. Data de nascimento tem `@PastOrPresent` para evitar cadastros com data futura. Sexo é um enum (`MALE` ou `FEMALE`), qualquer outro valor retorna 400.

### Contratação de um plano

```http
POST /contratacoes
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "statusId": 1,
    "paymentMethodId": 1
}
```

Esta é a entidade mais rica do sistema. Quatro FKs todas validadas, e uma regra de negócio importante: **o `contractedValue` não vem do cliente, vem derivado do `Plan.monthlyValue` no momento da criação**. Isso evita que alguém envie `"contractedValue": 0.01` tentando contratar o plano Total por um centavo. A coluna `startDate` é preenchida automaticamente com a data atual via `@CreationTimestamp`.

A resposta traz toda a hierarquia aninhada (Pet com Owner, Owner com Cidade, Cidade com Estado, etc), o que pode parecer pesado mas é útil para o front renderizar a tela de detalhes sem precisar fazer várias chamadas.

### Trocar status de uma contratação

```http
PUT /contratacoes/1
Content-Type: application/json

{
    "petId": 1,
    "planId": 1,
    "statusId": 2,
    "paymentMethodId": 1
}
```

O caso típico é mudar o status (ACTIVE → CANCELED, SUSPENDED, OVERDUE). Se o cliente trocar `planId` também, o `contractedValue` é **recalculado** automaticamente — não é permitido mudar de plano sem atualizar o valor.

### Filtros úteis em contratações

```http
GET /contratacoes?statusId=1                 # todas ativas
GET /contratacoes?petId=1                    # histórico de contratações de um pet
GET /contratacoes?planId=5&statusId=2        # cancelamentos do plano Total
```

Combinável com paginação e ordenação via Spring Pageable.

### Lookups (Estados, Cidades, Espécies, etc)

Esses são endpoints de leitura para o front popular dropdowns. Todos suportam GET de listagem completa (sem paginação) e GET por ID:

- `GET /estados`
- `GET /cidades`
- `GET /especies`
- `GET /racas`
- `GET /planos`
- `GET /formas-pagamento`
- `GET /status-contratacao`

CRUD completo (POST, PUT, DELETE) também existe nessas rotas para operações administrativas, mas o front normalmente só consome os GETs.

## Testando localmente

A pasta `docs/` contém:

- `clyvo-care-api.yaml` — collection completa do Insomnia, organizada em pastas numeradas por entidade. Importe no Insomnia 8+ via Application > Import > File. Cada pasta tem requests de seed, GET, validação de erros, etc.
- `MER.png` — diagrama entidade-relacionamento gerado no Oracle Data Modeler, útil para visualizar as FKs e o ciclo de vida das tabelas.
- `fix.sql` — script completo de criação do schema Oracle (14 tabelas, 13 procedures com tratamento de exceção, carga de dados de exemplo e blocos PL/SQL analíticos). Schema em inglês para casar com as entidades JPA, mensagens de erro e relatórios em português para facilitar a leitura.

Para um teste end-to-end rápido, com a aplicação rodando:

1. Importe o YAML do Insomnia
2. Crie uma City via POST (já tem 12 no seed, mas pode criar outra)
3. Crie um Owner referenciando o `cityId`
4. Crie um Pet referenciando `ownerId`, `speciesId` e `breedId`
5. Crie uma Subscription referenciando `petId`, `planId`, `statusId`, `paymentMethodId`
6. Liste contratações filtrando por `petId` para ver o histórico

Se algum 404 aparecer com mensagem amigável ("Owner with ID 99 not found"), o tratamento de erros está funcionando. Se aparecer um JSON gigante com stack trace, alguma coisa escapou do `ValidationHandler` — abra issue ou ajuste o handler.

## Estrutura de pastas

```
src/main/java/br/com/fiap/ClyvoCareAPI/
├── ClyvoCareApiApplication.java
├── config/
│   └── OpenApiConfig.java
├── controller/
│   ├── StateController.java
│   ├── CityController.java
│   ├── SpeciesController.java
│   ├── BreedController.java
│   ├── PaymentMethodController.java
│   ├── SubStatusController.java
│   ├── PlanController.java
│   ├── OwnerController.java
│   ├── PetController.java
│   └── SubscriptionController.java
├── service/
│   └── (um para cada controller)
├── repository/
│   └── (um por entidade)
├── entity/
│   ├── State.java, City.java
│   ├── Species.java, Breed.java
│   ├── PaymentMethod.java, SubStatus.java
│   ├── Plan.java, Owner.java
│   ├── Pet.java, Sex.java (enum)
│   └── Subscription.java
├── dto/
│   ├── XxxRequest.java (record com Bean Validation)
│   └── XxxResponse.java (record com fromEntity static)
└── validation/
    └── ValidationHandler.java (@RestControllerAdvice global)

docs/
├── clyvo-care-api.yaml
├── fix.sql
└── MER.png

src/main/resources/
└── application.properties (configuração Oracle e Spring)
```

## Schema do banco

O banco é dropado e recriado pelo arquivo `docs/fix.sql`. Ele contém:

- DDL das 14 tabelas (cadastro + saúde + log de erros)
- 13 stored procedures de inserção com tratamento de exceção
- Carga de dados de exemplo (8 estados, 12 cidades, 4 espécies, 12 raças, 5 planos, etc)
- Blocos de análise PL/SQL (joins, LAG/LEAD, cursores explícitos com decisão)

Para conectar a API a um Oracle limpo, basta rodar o `docs/fix.sql` no SQL Developer logado com seu RM. O schema é validado pelo Hibernate na inicialização (`spring.jpa.hibernate.ddl-auto=validate`), então se algo não bater entre entidade e tabela, a aplicação falha logo no startup com mensagem clara.

## Requisitos da disciplina cobertos

A entrega cumpre, dentro desta API:

- CRUD completo das 10 entidades com retornos HTTP corretos (200, 201, 204, 400, 404, 409)
- Bean Validation com extensões brasileiras (`@CPF`, `@Email`, `@Size`, `@Positive`, `@PastOrPresent`)
- Paginação e ordenação via `Pageable` em Owner, Pet e Subscription
- Busca por parâmetros opcionais combinados (3 endpoints com query params)
- Pelo menos 3 consultas JPQL personalizadas (search em Owner, Pet, Subscription)
- Cache configurado em listagens estáveis
- Tratamento global de exceções com formato JSON consistente
- Separação clara entre Request DTO, Response DTO e Entity
- Documentação Swagger/OpenAPI completa com tags, summaries e ApiResponses

## Limitações conhecidas

- **Sem autenticação**. A API não tem login implementado. O `passwordHash` é guardado corretamente, mas não há endpoint `/login` nem token JWT. Decisão consciente: escopo da disciplina foca em REST/JPA, autenticação seria sprint separada.
- **Validação de FK entre APIs não acontece em tempo real**. Se a API C# deletar uma clínica enquanto o Java estiver consultando, pode ocorrer inconsistência momentânea. O Oracle resolve via constraints, mas a UX em casos de borda não é polida.
- **Migrations não automatizadas**. O schema é criado manualmente via `fix.sql`. Em um projeto real, usaríamos Flyway ou Liquibase. Para o escopo acadêmico, a abordagem de schema-as-contract é defensável.

## Próximos passos

Itens que não entraram nesta entrega mas fariam sentido evoluir depois:

- Autenticação via JWT
- Auditoria de mudanças (quem alterou o quê e quando)
- Soft delete em entidades sensíveis (Owner, Pet)
- Migrations versionadas (Flyway)
- Profile de teste com banco em memória para CI
- Métricas (Actuator + Prometheus)
