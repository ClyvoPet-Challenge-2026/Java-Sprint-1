# ClyvoCare API

API REST para cadastrar tutores, pets e planos de saúde, simular preços e acompanhar contratações. Construída em Spring Boot com Oracle, faz parte do Challenge FIAP 2026, turma 2TDSPG.

Com ela, você pode consultar o catálogo de planos, contratar um plano para um pet e acompanhar seu status. Esta API complementa a [API .NET](https://github.com/ClyvoPet-Challenge-2026/.Net-Sprint-1), responsável pela parte de clínicas veterinárias do projeto.

A proposta é reunir o cadastro do tutor, seus pets e suas contratações no mesmo fluxo. Para o tutor, isso permite consultar os planos ativos e seus valores. Para a administração, permite acompanhar contratações, alterar planos e registrar pausas ou encerramentos.

## Como os projetos se complementam

O ClyvoCare está dividido em repositórios que atendem a partes diferentes da solução e das entregas do Challenge:

| Projeto | Papel |
|---|---|
| [API Java](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1) | Cadastro de tutores, pets e planos; autenticação; simulação e gestão de contratações |
| [API .NET](https://github.com/ClyvoPet-Challenge-2026/.Net-Sprint-1) | Cadastro e consulta de clínicas veterinárias, com leitura de estados e cidades compartilhados |
| [Clyvo-Care — mobile](https://github.com/ClyvoPet-Challenge-2026/Clyvo-Care) | Versão mobile das entregas, com a apresentação visual voltada à experiência do aplicativo |
| [ClyvoCare Web](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web) | Camada de visualização em React e Vite criada para demonstrar os fluxos exigidos em Java Advanced |

As APIs Java e .NET trabalham com partes complementares do domínio. A Java mantém os estados e as cidades; a .NET consulta essas tabelas pelo Entity Framework e vincula as clínicas às cidades existentes. Para compartilhar esses dados, configure as duas APIs para usar o mesmo schema Oracle.

Na versão atual da .NET, estão implementados o CRUD de clínicas, as consultas de estados e cidades e a criação e consulta de agendamentos com isolamento por tutor. A auditoria de alterações em clínicas usa MongoDB Atlas. Lembretes ainda não têm endpoints implementados.

Para conhecer a versão mobile e sua apresentação visual, acesse o repositório **Clyvo-Care**. As instruções de frontend deste README são para o **ClyvoCare Web**, a interface de apoio à entrega de Java Advanced. As duas interfaces correspondem a entregas distintas: frontend em Java Advanced e aplicativo em Mobile Application Development, conforme o enunciado da Sprint 3.

## API publicada na Azure

| Endereço | Uso |
|---|---|
| [Swagger](https://clyvocare-java-rm561584.azurewebsites.net/swagger-ui/index.html) | Documentação e execução das operações |
| [Contrato OpenAPI](https://clyvocare-java-rm561584.azurewebsites.net/v3/api-docs) | Contrato HTTP em JSON |
| [Estados](https://clyvocare-java-rm561584.azurewebsites.net/estados) | Consulta pública no Oracle FIAP |

A Java usa Oracle FIAP remoto e emite os JWTs aceitos também pela API .NET. O deploy é nativo em um Web App Linux Java 17, no grupo `rm561584`, compartilhando um plano B2 com a .NET. O MongoDB Atlas pertence ao contexto de auditoria/importação da .NET; a Java não precisa de uma conexão MongoDB.

A pipeline [clyvocare-java](https://dev.azure.com/RM561584/Sprint%204%20%E2%80%93%20Azure%20DevOps/_build?definitionId=1) publica automaticamente após build, testes e geração do artefato na `master`. A execução [6](https://dev.azure.com/RM561584/Sprint%204%20%E2%80%93%20Azure%20DevOps/_build/results?buildId=6) concluiu CI e CD com sucesso. Na validação integrada de 02/11/2026, foram confirmados login, uso do JWT na .NET, contratações e relatórios com os packages Oracle. Consulte a execução mais recente para conferir o estado atual do deploy.

## Como executar

Você pode rodar a API na sua máquina usando o Oracle da FIAP. Para desenvolver localmente, siga os passos abaixo. A publicação nativa na Azure segue [Inicialização no Azure App Service](#inicialização-no-azure-app-service), com provisionamento compartilhado e pipelines CI/CD.

Para rodar a API na sua máquina, você precisa de:

- **JDK 17 ou superior**, com `JAVA_HOME` configurado.
- **Acesso a um banco Oracle**, com usuário e senha. Você pode usar o Oracle da FIAP com as credenciais da sua conta.
- **Git**, para clonar o repositório.

O Maven Wrapper já está incluído. Você não precisa instalar o Maven separadamente. Docker não é necessário para executar a API localmente nem para a publicação nativa no App Service.

### Configurando a conexão

Clone o projeto e entre na pasta:

```bash
git clone --branch master https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1.git
cd Java-Sprint-1
```

Após clonar, gere o par RSA uma única vez, na raiz deste repositório, com `bash scripts/generate-keys.sh`. É necessário OpenSSL, disponível também no Git Bash. O comando cria os dois PEMs em `.keys` e recusa sobrescrever arquivos existentes. Entregue somente a pública correspondente à API .NET; não gere um par separado para ela. Os caminhos externos estão documentados na seção de autenticação.

Defina a conexão no mesmo terminal em que você vai iniciar a aplicação. Substitua os valores de usuário e senha pelos da sua conta Oracle.

No **Bash ou Git Bash**:

```bash
export SPRING_DATASOURCE_URL='jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL'
export SPRING_DATASOURCE_USERNAME='SEU_USUARIO'
export SPRING_DATASOURCE_PASSWORD='SUA_SENHA'
./mvnw spring-boot:run
```

No **PowerShell**:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL'
$env:SPRING_DATASOURCE_USERNAME = 'SEU_USUARIO'
$env:SPRING_DATASOURCE_PASSWORD = 'SUA_SENHA'
.\mvnw.cmd spring-boot:run
```

Se você inicia a API pela IDE, configure essas mesmas variáveis na configuração de execução. Para usar outro Oracle, ajuste também a URL JDBC.

As credenciais acima conectam a aplicação ao banco. O login da interface usa os usuários cadastrados em `TB_CAD_OWNER`, apresentados na próxima seção.

### O que acontece ao iniciar

Na execução normal, o Flyway aplica as migrations pendentes antes de o Hibernate validar as tabelas. Em um schema vazio da aplicação, ele cria a estrutura e carrega os dados de demonstração. Você não precisa executar o script SQL manual.

Se o schema já tem tabelas, leia [Banco de dados e migrations](#banco-de-dados-e-migrations): o projeto usa baseline para trabalhar com bancos existentes.

Depois da inicialização, os endereços são:

| Endereço | Uso |
|---|---|
| `http://localhost:8080` | Base da API |
| `http://localhost:8080/swagger-ui.html` | Documentação interativa |
| `http://localhost:8080/v3/api-docs` | Contrato OpenAPI em JSON |

A raiz da API não é uma página de apresentação. Para navegar pelos fluxos da entrega de Java Advanced, inicie também a interface web descrita abaixo. As instruções da versão mobile ficam no repositório [Clyvo-Care](https://github.com/ClyvoPet-Challenge-2026/Clyvo-Care).

### Acessando a camada de visualização de Java Advanced

Em outro terminal, clone o [ClyvoCare Web](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web). Essa interface React/Vite consome a API Java para demonstrar login, pets e contratações. Você precisa de Node.js compatível com o Vite atual do projeto, por exemplo Node 22.12 ou superior, e npm.

```bash
git clone https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web.git
cd Java-Sprint-1-Web
cp .env.example .env
```

No PowerShell, você pode usar `Copy-Item .env.example .env`. No arquivo `.env`, configure:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Depois, instale as dependências e inicie a interface:

```bash
npm install
npm run dev
```

Acesse `http://localhost:5173`. As contas de demonstração são:

| E-mail | Senha inicial | Perfil |
|---|---|---|
| `ana@email.com` | `senha123` | `ADMIN` |
| `carlos@email.com` | `senha123` | `OWNER` |

A carga preserva senhas já personalizadas. A senha acima vale para contas criadas pela carga e para os placeholders antigos corrigidos pela V3.

Na interface, você encontra o cadastro de pets, a simulação e contratação de planos e a página **Meus planos**, que mostra as contratações ativas dos seus pets. O perfil `ADMIN` também tem acesso à gestão de status das contratações.

O CORS permite `http://localhost:5173` e `http://localhost:3000` por padrão. Se você usar outro endereço ou porta, defina `CORS_ALLOWED_ORIGINS` na API com a origem exata do frontend. Para mais de uma origem, separe os endereços por vírgula.

## Banco de dados e migrations

As migrations ficam em [src/main/resources/db/migration](src/main/resources/db/migration). Na execução normal, elas controlam a estrutura e a carga inicial; o Hibernate usa `ddl-auto=validate` para conferir se o banco corresponde às entidades.

| Migration | Responsabilidade |
|---|---|
| `V1__create_baseline_schema.sql` | Cria a estrutura inicial, com 15 tabelas |
| `V2__subscription_enums.sql` | Converte status e pagamento para texto, remove duas tabelas auxiliares e atualiza funções, procedimentos e trigger |
| `V3__seed_demo_data.sql` | Inclui os dados de demonstração ausentes e corrige as contas legadas de Ana e Carlos |
| `V4__create_packages.sql` | Organiza JSON, cálculos, relatórios e auditoria em packages e substitui as rotinas antigas |
| `V5__escape_json_control_characters.sql` | Corrige o escape dos caracteres de controle no JSON, preservando os checksums anteriores |

Após a V2, o schema tem 13 tabelas de aplicação. Ele inclui `TB_CAD_CLINIC`, usada pela API .NET, e as tabelas de eventos clínicos e lembretes previstas para a evolução dessa área. A existência dessas tabelas não significa que todos os respectivos endpoints já estejam implementados.

### Usando um banco que já tem dados

Com `spring.flyway.baseline-on-migrate=true`, um schema preenchido e ainda sem histórico do Flyway recebe um baseline na versão 1. Nesse caso, a V1 é considerada anterior ao ponto de partida e as migrations seguintes são aplicadas.

Isso pressupõe que as tabelas existentes sejam compatíveis com o modelo esperado. O baseline não reconstrói uma estrutura incompleta. A V2 trata algumas diferenças legadas, como a ausência de `ROLE_NAME` e `ENABLED`, e interrompe a conversão se encontrar status ou pagamentos sem correspondência.

O Flyway registra as versões aplicadas em `flyway_schema_history`. Reiniciar a API não repete a carga já registrada. Para alterar uma migration aplicada, crie uma nova versão; editar V1 ou V2 muda o checksum que o Flyway usa para validar o histórico.

### Como os dados de demonstração são carregados

A V3 inclui estados, cidades, espécies, raças, planos e dois usuários. Ela procura cada registro por seus campos de identificação, como UF, nome ou e-mail, sem depender de IDs fixos. Assim, também consegue completar tabelas parcialmente preenchidas.

Nos registros existentes, a migration preserva os cadastros e preços. Para as contas de demonstração, ela:

- Substitui somente `hash1` de Ana e `hash2` de Carlos por BCrypt de `senha123`.
- Ajusta Ana para `ADMIN` e Carlos para `OWNER`.
- Preserva outras senhas e a situação de habilitação das contas existentes.

O `DataInitializer` fica como alternativa para ambientes com Flyway explicitamente desabilitado. A propriedade `app.seed.enabled=false` desativa essa alternativa Java; ela não desativa a V3. O deploy App Service mantém o Flyway habilitado e desativa essa carga alternativa.

### Script SQL e objetos de banco

[script_bd.sql](script_bd.sql) e [docs/script.sql](docs/script.sql) são os scripts completos da entrega de Database. Eles contêm criação de tabelas, dados e PL/SQL, incluindo comandos de exclusão de tabelas. **Você não precisa executá-los para iniciar a API com Flyway.**

O diagrama de modelagem está em [docs/MER.png](docs/MER.png). Após aplicar a migration V4 pelo Flyway, os objetos de banco incluem:

| Objeto | Uso |
|---|---|
| `PKG_JSON.SUBSCRIPTION_TO_JSON` | Monta o JSON de uma contratação com seus relacionamentos |
| `PKG_FINANCEIRO.CALCULATE_CONTRACT_VALUE` | Calcula o valor do plano conforme o pagamento |
| `PKG_RELATORIOS.REPORT_SUBSCRIPTIONS` | Retorna contratações em JSON, com filtro de status |
| `PKG_RELATORIOS.REPORT_REVENUE` | Retorna valores agrupados por plano e status, subtotais e total geral |
| `TRG_SUBSCRIPTION_AUDIT` | Registra inclusões, alterações e exclusões de contratações |

O cálculo usado pela API está em `ContractPricingService`. A função SQL mantém as mesmas taxas para uso no banco; o serviço Java não chama essa função.

A V5 atualiza somente o corpo de `PKG_JSON`. Além de aspas, barras, tabulação e quebras de linha, o escape cobre todos os caracteres de controle de código 0 a 31. Esses caracteres podem chegar pelos campos de texto; sem escape, o JSON produzido pelo Oracle pode ser recusado pelo leitor da aplicação. A atualização não altera tabelas nem regrava cadastros existentes.

Após aplicar a V5, o [teste SQL de escape](docs/tests/json-escaping.sql) confere o retorno do texto original e a ausência de controles sem escape. No SQL*Plus, usando o usuário/schema da aplicação:

```bash
sqlplus -L 'APP_USER@//HOST:1521/XEPDB1' @docs/tests/json-escaping.sql
```

Informe a senha quando solicitada e ajuste host/serviço para o ambiente. O teste retorna sucesso para aspas, barras, Unicode e os 32 caracteres de controle; falhas encerram o script com erro. Ele não cria tabelas nem grava dados da aplicação. Esse teste usa Oracle real e é separado da suíte Maven com H2.

### Relatórios administrativos via Oracle

Os endpoints abaixo exigem token de `ADMIN` e executam as procedures do package `PKG_RELATORIOS` por JDBC. Não usam consultas JPA como substituição quando o package está ausente.

| Método e rota | Parâmetros | Resposta |
|---|---|---|
| `GET /relatorios/contratacoes` | `status` opcional: `ACTIVE`, `INACTIVE` ou `PENDING` | `items` com o JSON produzido pelo Oracle e `totalCount` |
| `GET /relatorios/receita` | `minValue` opcional, padrão `0`, de `0` a `99999999.99`, com até duas casas decimais | `items` com linhas de detalhe/subtotal e `grandTotal` |

As linhas de receita têm `lineType` (`DETAIL` ou `SUBTOTAL`), `planName`, `status`, `quantity` e `amount`. O status de um subtotal é nulo. O total geral vem separado em `grandTotal`; não some novamente as linhas de subtotal aos detalhes. O relatório de contratações mantém os campos da função SQL, incluindo dados do tutor, e fica restrito à administração.

Exemplos no Bash, usando um token de administrador obtido no login:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" 'http://localhost:8080/relatorios/contratacoes?status=ACTIVE'
curl -H "Authorization: Bearer $ADMIN_TOKEN" 'http://localhost:8080/relatorios/receita?minValue=10.50'
```

Sem registros correspondentes, as procedures sinalizam os códigos Oracle `20031` ou `20041`, e a API responde `200` com lista vazia e total zero. Parâmetros inválidos retornam `400`, visitante recebe `401` e tutor recebe `403`. Falha de execução, package indisponível ou JSON inválido retornam `503` com mensagem genérica; registros de erro embutidos no JSON não são apresentados como sucesso.

Antes de demonstrar, confira as versões 4 e 5 em `flyway_schema_history` e os packages/trigger em `USER_OBJECTS` e `USER_ERRORS`. O Flyway aplica as migrations na inicialização normal e no App Service; os scripts destrutivos de demonstração não são necessários para essa atualização.

## Autenticação e permissões

Para obter um token, envie e-mail e senha:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "carlos@email.com",
  "password": "senha123"
}
```

A resposta contém um campo `token`. Copie o valor e envie nas próximas requisições:

```http
Authorization: Bearer <token>
```

O JWT vale por 30 minutos e é assinado com RSA (RS256). As chaves são carregadas de arquivos externos: `.keys/private_key.pem` e `.keys/public_key.pem`, relativos à pasta de execução da API. Esses arquivos não entram no Git nem na imagem Docker. As senhas são armazenadas com BCrypt, e o login também verifica se a conta está habilitada.

Para usar outro caminho, defina `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY` como localizações de arquivo, por exemplo `file:/run/secrets/private_key.pem` e `file:/run/secrets/public_key.pem`. As variáveis recebem o caminho, não o conteúdo PEM. A API precisa dos dois arquivos para iniciar; a API .NET deve receber somente a pública correspondente.

Se ainda não tiver um par de chaves, gere-o no Bash ou Git Bash com OpenSSL, a partir da raiz do repositório:

```bash
bash scripts/generate-keys.sh
```

Para gerar em outro diretório, passe o destino como primeiro argumento, por exemplo `bash scripts/generate-keys.sh /CAMINHO/segredos/jwt`. Guarde esse par fora da imagem e mantenha os mesmos arquivos entre reinicializações. Em containers, configure os caminhos montados com `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY`; a .NET usa `Jwt__PublicKeyPath` para a mesma pública.

Não sobrescreva um par em uso sem atualizar os serviços que o utilizam. O par anteriormente versionado permanece no histórico do Git e deve ser substituído antes de publicar o ambiente; remover os arquivos da revisão atual não revoga tokens. A troca de par exige novo login dos usuários.

Para App Service, configure os PEMs em Base64 conforme a seção de inicialização abaixo. O provisionamento entrega privada e pública à Java e somente a mesma pública à .NET, sem incluir chaves nos artefatos.

Use `GET /auth/me` para consultar os dados do usuário autenticado. O cadastro público em `POST /responsaveis` cria contas com perfil `OWNER`.

| Recurso | Consulta | Criação | Edição e exclusão |
|---|---|---|---|
| Responsáveis | `ADMIN` lista todos; `OWNER` consulta o próprio perfil | Pública | `ADMIN` ou o próprio `OWNER`; exclusão desativa a conta |
| Pets | `ADMIN` ou o próprio `OWNER` | `ADMIN` ou `OWNER`, respeitando o tutor | `ADMIN` ou o próprio `OWNER` |
| Contratações | `ADMIN` ou o próprio `OWNER` | `ADMIN` ou `OWNER`, respeitando o pet | Somente `ADMIN`, incluindo cancelamento e alterações de status |
| Estados e cidades | Pública | `ADMIN` | `ADMIN` |
| Espécies, raças e planos | Usuário autenticado | `ADMIN` | `ADMIN` |

A simulação aceita `ADMIN` e `OWNER`. Cancelar uma contratação, alterar seu status ou trocar o plano exige `ADMIN`. Um tutor `OWNER` recebe `403` ao tentar cancelar, inclusive quando a contratação é sua; o registro permanece inalterado. O Swagger, o login, o cadastro de tutor e as leituras de estados/cidades são públicos.

A consulta `GET /contratacoes/minhas` retorna somente os planos ativos da conta autenticada. As listagens gerais de pets e contratações também restringem os dados ao tutor; `ADMIN` mantém acesso administrativo. Indicar outro tutor em uma operação de proprietário retorna `403`, sem alterar os registros.

O token inclui `ownerId`, e-mail, perfil, emissor e expiração. A API verifica a assinatura e confere a conta habilitada em cada requisição autenticada, inclusive de `ADMIN`. Tokens antigos sem `ownerId` exigem novo login. Alterar o e-mail ou desativar a conta invalida o uso do token atual. Alterar apenas a senha não revoga os tokens já emitidos antes de expirarem.

`DELETE /responsaveis/{id}` desativa a conta com `204`, preservando pets, contratações e histórico. O tutor só pode desativar a própria conta; `ADMIN` pode desativar outras. O aplicativo deve encerrar a sessão após a confirmação. Não há reativação pelo endpoint público de cadastro.

## Fluxos de contratação

### Simular e contratar um plano

Primeiro, consulte `GET /planos` e escolha um plano. Os IDs variam conforme o banco, então use os valores retornados pela API nos exemplos abaixo.

```http
POST /contratacoes/simulacao
Authorization: Bearer <token>
Content-Type: application/json

{
  "planId": 1,
  "paymentMethod": "PIX"
}
```

A simulação devolve o valor base, a taxa de desconto, o desconto em reais e o valor final. Ela não cria uma contratação.

| Pagamento | Valor enviado na API | Desconto |
|---|---|---|
| Pix | `PIX` | 5% |
| Cartão de débito | `DEBIT_CARD` | 3% |
| Cartão de crédito | `CREDIT_CARD` | Sem desconto |
| Boleto | `BOLETO` | Sem desconto |

Por exemplo, um plano de R$ 69,90 pago por Pix fica em R$ 66,41. O serviço arredonda o valor final para duas casas e calcula o desconto em reais pela diferença, R$ 3,49 nesse caso.

Para confirmar, envie também o pet:

```http
POST /contratacoes
Authorization: Bearer <token>
Content-Type: application/json

{
  "petId": 1,
  "planId": 1,
  "paymentMethod": "PIX"
}
```

A API consulta o preço novamente, calcula o valor e cria a contratação com status `ACTIVE`. O frontend não define o preço final nem o status inicial.

Um pet só pode receber uma contratação ativa por esses fluxos. Se já houver outra, a API responde `409 Conflict`. Depois de contratar, você pode consultar `GET /contratacoes/minhas` ou abrir **Meus planos** na interface.

### Pausar, encerrar ou trocar um plano

O administrador pode alterar o status com `PATCH /contratacoes/{id}/status`:

```json
{
  "status": "PENDING"
}
```

As transições permitidas são:

| Status atual | Pode mudar para |
|---|---|
| `ACTIVE` — ativo | `PENDING` ou `INACTIVE` |
| `PENDING` — pendente ou pausado | `ACTIVE` ou `INACTIVE` |
| `INACTIVE` — encerrado | Nenhum |

Repetir o status atual ou tentar reativar uma contratação encerrada retorna `409`. Para um pet cujo contrato foi encerrado, você pode criar outra contratação.

A troca de plano usa `POST /contratacoes/{id}/troca-plano`:

```json
{
  "planId": 2,
  "paymentMethod": "DEBIT_CARD"
}
```

Se você omitir `paymentMethod` ou enviar `null`, o pagamento atual é mantido. A operação recalcula o preço e preserva pet, status e data de início.

O `PUT /contratacoes/{id}` também recalcula o preço, recebe `petId`, `planId` e `paymentMethod`, e preserva o status. Tanto o PUT quanto a troca de plano aceitam somente contratações ativas ou pendentes.

## Endpoints

O contrato completo, com campos e respostas, está no Swagger. Esta tabela reúne os recursos para você localizar as operações:

| Rota base | Recurso | Listagem |
|---|---|---|
| `/responsaveis` | Tutores | Paginada |
| `/pets` | Pets | Paginada |
| `/contratacoes` | Contratações | Paginada |
| `/estados` | Estados | Lista |
| `/cidades` | Cidades | Lista |
| `/especies` | Espécies | Lista |
| `/racas` | Raças | Lista |
| `/planos` | Catálogo de planos | Lista |

Esses recursos têm `POST` e `GET` na rota base, além de `GET`, `PUT` e `DELETE` em `/{id}`, conforme as permissões descritas acima.

As rotas específicas são:

| Método | Rota | Uso |
|---|---|---|
| `POST` | `/auth/login` | Obter o token |
| `GET` | `/auth/me` | Consultar o usuário do token |
| `GET` | `/contratacoes/minhas` | Listar os planos ativos dos pets do usuário, com paginação |
| `POST` | `/contratacoes/simulacao` | Simular o valor |
| `PATCH` | `/contratacoes/{id}/status` | Alterar o status |
| `POST` | `/contratacoes/{id}/troca-plano` | Trocar o plano |
| `GET` | `/formas-pagamento` | Consultar os valores de pagamento aceitos |
| `GET` | `/status-contratacao` | Consultar os status aceitos |

Status e formas de pagamento são enums. Suas rotas retornam listas fixas e não têm cadastro, edição ou exclusão.

### Cadastro de pet

Para cadastrar, informe o ID do tutor e da espécie. A raça é opcional:

```json
{
  "name": "Rex",
  "birthDate": "2020-03-10",
  "sex": "MALE",
  "ownerId": 1,
  "speciesId": 1,
  "breedId": null
}
```

Envie esse corpo para `POST /pets`, com o token. Você obtém seu próprio `ownerId` em `GET /auth/me` e os IDs de espécies e raças em suas respectivas consultas.

A data de nascimento não pode estar no futuro. Sexo aceita `MALE` ou `FEMALE`, e os IDs informados devem ser positivos.

### Filtros e paginação

As páginas começam em zero. Você pode combinar os filtros do recurso com `page`, `size` e `sort`:

```http
GET /responsaveis?name=ana&page=0&size=10&sort=name,asc
GET /pets?ownerId=2&speciesId=1&page=0&size=10
GET /contratacoes?petId=1&status=ACTIVE
GET /contratacoes/minhas?page=0&size=6&sort=id,desc
```

| Recurso | Filtros disponíveis |
|---|---|
| Responsáveis | `name`, `cpf`, `email` |
| Pets | `name`, `ownerId`, `speciesId`, `breedId` |
| Contratações | `petId`, `planId`, `status`, `paymentMethod` |

As respostas paginadas trazem os registros em `content` e informações como `totalElements`, `totalPages` e `last`.

## Consultando e verificando a API

### Swagger, Insomnia ou curl

O Swagger permite consultar o contrato e enviar requisições. Use o botão **Authorize** para informar o JWT retornado pelo login, sem o prefixo `Bearer`. Para os relatórios, faça login com uma conta `ADMIN`. Você também pode usar Insomnia ou curl.

A coleção [docs/clyvo-care-api.yaml](docs/clyvo-care-api.yaml) tem os ambientes **Base Environment**, para localhost, e **Azure App Service**. Ajuste `base_url` para a URL HTTPS retornada pelo provisionamento e execute o login da pasta `00 - Autenticacao`. O script da requisição salva o JWT em `access_token`.

As pastas seguintes agrupam consultas, cadastros, simulação, contratação e mudanças de status. Os cadastros de responsável, pet e contratação guardam os IDs retornados para as próximas requisições. Alguns exemplos de catálogo usam IDs fixos; confira se existem no seu banco antes de executá-los.

Se preferir curl, faça o login:

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"carlos@email.com","password":"senha123"}'
```

Copie o token da resposta e use-o na consulta:

```bash
curl -i http://localhost:8080/contratacoes/minhas \
  -H 'Authorization: Bearer COLE_O_TOKEN_AQUI'
```

Esses exemplos usam Bash. No PowerShell, use `curl.exe` e escreva cada comando em uma linha.

### Testes disponíveis

Os testes verificam a inicialização do contexto, assinatura/verificação de JWT, permissões de dois tutores e administrador, isolamento de pets e contratações, edição de perfil, cancelamento e desativação de conta. Também conferem os descontos da simulação e a recusa de contratação ativa duplicada. A configuração compartilhada usa H2 em memória e um par RSA temporário, com Flyway e carga de demonstração desabilitados; não usa o Oracle nem as chaves locais da aplicação. Esses testes não validam migrations, concorrência real ou particularidades do Oracle.

Os testes dos relatórios usam JDBC simulado para conferir parâmetros, leitura do CLOB/cursor, valores decimais, códigos de ausência de dados e liberação de recursos. Os testes HTTP conferem autorização, parâmetros, respostas e tratamento de falhas. Eles não executam os packages no Oracle; essa evidência deve ser obtida no ambiente com V4 aplicada.

Uma validação integrada adicional foi executada com instalação limpa em Oracle XE 21c e atualização do schema existente no Oracle 19c da FIAP. Foram conferidos login Java e uso do JWT na .NET, persistência dos agendamentos, disputa por pet/data em conexões independentes, isolamento entre tutores, relatórios CLOB/cursor, auditoria, perfil, cancelamento e desativação. A V5 também foi conferida com caracteres de controle em um cadastro real. Os registros temporários foram removidos, preservando os cadastros anteriores; os logs de auditoria e de erros esperados da validação permanecem. Isso não substitui a verificação do deploy e do aplicativo no dispositivo.

Para executar a suíte, use `./mvnw test` ou `.\mvnw.cmd test`.

O gerador RSA tem uma verificação separada: `bash scripts/generate-keys.test.sh`. Ela cria um par temporário, confere sua correspondência e verifica a recusa de sobrescrita, sem modificar as chaves locais da aplicação.

A inicialização do App Service pode ser verificada com `bash scripts/start-appservice.test.sh`. O teste usa chaves temporárias e simula o executável Java para conferir configurações obrigatórias, Base64 inválido, PEM inválido, pares diferentes, reinicialização com o mesmo par e propagação de falha do processo. No Linux, também verifica as permissões dos arquivos. Ele não acessa o Oracle nem provisiona recursos Azure.

A suíte Java também verifica que a documentação da API apresenta o endereço HTTPS público quando o perfil `appservice` recebe os cabeçalhos encaminhados pelo proxy.

## Inicialização no Azure App Service

O script [scripts/start-appservice.sh](scripts/start-appservice.sh) prepara a execução nativa da API em **App Service Linux com Java SE 17**, sem publicar uma imagem Docker própria. O provisionamento compartilhado e a pipeline estão descritos abaixo. O ambiente RM561584 já foi provisionado e validado; siga essas instruções para reproduzir a instalação em outro ambiente.

O pacote de publicação deve conter o JAR executável como `app.jar` e o script em `scripts/start-appservice.sh`. O comando de inicialização do Web App será:

```bash
bash /home/site/wwwroot/scripts/start-appservice.sh
```

Configure as seguintes variáveis nas **configurações de aplicativo** do Web App:

| Variável | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do Oracle compartilhado com a .NET, por exemplo `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do schema Oracle |
| `SPRING_DATASOURCE_PASSWORD` | Senha do schema Oracle |
| `JWT_PRIVATE_KEY_BASE64` | Conteúdo completo do PEM privado codificado em Base64, em uma linha |
| `JWT_PUBLIC_KEY_BASE64` | Conteúdo completo do PEM público correspondente codificado em Base64, em uma linha |
| `CORS_ALLOWED_ORIGINS` | Origens HTTPS das interfaces web, separadas por vírgula |
| `JAVA_OPTS` | Opções da JVM, por exemplo `-Xms256m -Xmx768m` para limitar o heap no plano compartilhado |

Gere um par RSA próprio para o deploy uma única vez com `scripts/generate-keys.sh`, em um diretório protegido fora do pacote. Mantenha o mesmo par entre publicações e reinicializações e forneça somente a pública correspondente à .NET. Os dois valores Base64 devem ser tratados como segredos: Base64 não é criptografia. Não coloque PEMs, senhas ou arquivos de configuração com segredos no Git, nos artefatos ou nos logs. Não use parâmetros da JVM para transportar credenciais.

O script recusa iniciar sem a conexão explícita e os dois PEMs válidos do mesmo par RSA. Ele grava as chaves em um diretório temporário privado, exporta os caminhos esperados pela aplicação e remove os valores Base64 do ambiente do processo Java. Uma falha na preparação remove os arquivos criados; após iniciar a JVM, os PEMs permanecem no diretório temporário da instância. Não há geração automática de novas chaves a cada início.

O perfil `appservice` desabilita a carga de demonstração, usa a porta fornecida pela plataforma (`SERVER_PORT`, ou `PORT`, com padrão `8080`) e interpreta os cabeçalhos encaminhados pelo proxy HTTPS. O Flyway continua habilitado e o Hibernate continua em `validate`; não use `create` ou `update` no schema compartilhado. As opções de `JAVA_OPTS` são encaminhadas ao interpretador de opções da JVM por `JDK_JAVA_OPTIONS`, preservando opções já presentes e sem executar comandos de shell. O JAR usa `AZURE_JAVA_APP_PATH` quando fornecido pela plataforma, ou `/home/site/wwwroot/app.jar`.

Referências: [configuração Java no App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run) e [variáveis da plataforma](https://learn.microsoft.com/en-us/azure/app-service/reference-app-settings).

### Provisionamento compartilhado

O script [scripts/provision-appservice.sh](scripts/provision-appservice.sh) usa Azure CLI e o template [infra/appservice.json](infra/appservice.json) para criar um grupo dedicado, **um plano Linux B2 com uma instância** e **dois Web Apps**, Java e .NET, no mesmo plano. Registra o provedor `Microsoft.Web` e configura HTTPS, inicialização, Oracle, MongoDB e RSA. Este é o único provisionamento compartilhado: não execute uma segunda cópia pelo repositório .NET.

**Executar esse script cria recursos pagos.** O plano continua sendo cobrado mesmo se os Web Apps forem parados. Confirme crédito, disponibilidade regional e período de uso antes de executar; a configuração não impõe um teto automático de gastos. Não utilize o grupo de uma implantação antiga ou de outro projeto.

Pré-requisitos: Bash/Git Bash, Azure CLI atualizada, Node.js, OpenSSL, login na assinatura escolhida e permissão para criar os recursos. Prepare antes o **MongoDB Atlas Free**, com banco e usuário próprios; este template não cria uma conta nem um cluster Atlas e não substitui o Oracle da FIAP. Os scripts de coleções/importação estão em [docs/mongodb da .NET](https://github.com/ClyvoPet-Challenge-2026/.Net-Sprint-1/tree/develop/docs/mongodb). Prepare uma pública e uma privada RSA novas para o deploy, fora dos repositórios, com `scripts/generate-keys.sh`.

Na raiz do repositório Java, configure nomes exclusivos. Substitua os exemplos pelos nomes do seu ambiente:

```bash
export AZURE_SUBSCRIPTION_ID='ID_DA_ASSINATURA'
export AZURE_RESOURCE_GROUP='rm561584'
export AZURE_PLAN_NAME='asp-clyvocare-rm561584'
export JAVA_WEBAPP_NAME='clyvocare-java-rm561584'
export DOTNET_WEBAPP_NAME='clyvocare-dotnet-rm561584'
export AZURE_LOCATION='eastus'
export SPRING_DATASOURCE_URL='jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL'
export SPRING_DATASOURCE_USERNAME='SEU_RM'
export Mongo__DatabaseName='clyvocare'
export JWT_PRIVATE_KEY_BASE64="$(base64 < /CAMINHO/segredos/private_key.pem | tr -d '\r\n')"
export JWT_PUBLIC_KEY_BASE64="$(base64 < /CAMINHO/segredos/public_key.pem | tr -d '\r\n')"
```

Informe os valores secretos sem gravá-los no histórico do terminal:

```bash
read -r -s -p 'Senha Oracle: ' SPRING_DATASOURCE_PASSWORD; printf '\n'
export SPRING_DATASOURCE_PASSWORD
read -r -s -p 'Connection string Oracle completa da .NET: ' ConnectionStrings__ClyvoCareOracle; printf '\n'
export ConnectionStrings__ClyvoCareOracle
read -r -s -p 'URI MongoDB Atlas: ' Mongo__ConnectionString; printf '\n'
export Mongo__ConnectionString
```

A connection string .NET deve usar o **mesmo banco, usuário/schema e senha** da configuração Java. Para o App Service, use o descriptor com keepalive documentado no [README da .NET](https://github.com/ClyvoPet-Challenge-2026/.Net-Sprint-1/blob/master/README.md#api-publicada-na-azure), preservando `EXPIRE_TIME=1` e os limites de abertura de conexão. Para interfaces web, configure também `CORS_ALLOWED_ORIGINS` com as origens HTTPS separadas por vírgula, sem caminhos ou barra final. Sem essa variável, o provisionamento não habilita origens web; o aplicativo mobile nativo não depende de CORS.

Depois de revisar as configurações e autorizar o custo, execute:

```bash
bash scripts/provision-appservice.sh
```

O script valida o par RSA antes de alterar recursos e usa a assinatura explícita em todos os comandos, sem trocar a assinatura padrão da CLI. Se o grupo já existir, exige a tag `project=clyvocare-sprint4`; não adicione essa tag a um grupo alheio para contornar a proteção. A implantação usa modo incremental. Reexecutar atualiza as configurações dos Web Apps, portanto mantenha os mesmos nomes, os valores atuais e o mesmo par de chaves. Renomear recursos não remove os antigos; uma falha parcial também pode deixar recursos cobrados. Confira o estado no portal antes de repetir ou encerrar o ambiente. Não há exclusão automática. Não use `--debug`, `set -x` ou gravação do terminal com os segredos carregados.

Os parâmetros secretos são enviados como `secureObject`, por um arquivo temporário removido ao terminar, com permissão `600` no Linux. Não execute o gerador interno `appservice-parameters.cjs` diretamente no terminal: sua saída contém segredos. A saída normal do provisionamento mostra somente URLs e os IPs possíveis de saída da .NET. Configure esses IPs na lista de acesso do Atlas; não habilite acesso irrestrito como padrão. Confira se os IPs mudaram ao alterar o plano. Os valores podem ser retirados do ambiente do terminal depois do provisionamento.

O script não publica as aplicações, não altera o schema Oracle, não executa o SQL de recriação e não prepara as coleções MongoDB. O primeiro deploy das APIs será feito pelas pipelines, depois das migrations e coleções conferidas. O plano B2 tem recursos compartilhados; o heap Java é limitado a `768m`. A .NET permanece em Production. As URLs reais retornadas pela Azure são utilizadas, incluindo o hostname efetivo da .NET em `AllowedHosts`.

### CI/CD no Azure DevOps

A pipeline [azure-pipelines.yml](azure-pipelines.yml) executa a cada alteração na **master**. No CI, verifica os scripts, executa `mvnw verify` com JDK 17, publica resultados JUnit e o artefato `java/java.zip` no Azure DevOps. O ZIP contém somente `app.jar` e `scripts/start-appservice.sh`, sem pasta externa envolvendo o conteúdo. Não inclui chaves nem parâmetros secretos.

Após sucesso do CI na master, o CD baixa o artefato **da mesma execução**, publica no Web App usando Azure CLI e verifica `GET /estados` por HTTPS. Falhas de testes, publicação ou verificação HTTP fazem a pipeline falhar. Pull requests para master executam o CI, sem CD. O CD não recompila a aplicação nem executa o provisionamento de infraestrutura.

Antes de executar a pipeline:

1. Prepare a branch `master` a partir da versão validada, preservando o histórico, e faça commit/push pelo fluxo da equipe. Os repositórios atuais usam `develop`/`main`; apenas adicionar este YAML nessas branches não ativa o gatilho exigido.
2. Crie o projeto privado **Sprint 4 – Azure DevOps**, Git e Scrum/Agile, com a descrição exigida pelo professor. Preserve o projeto antigo de organização das tarefas. Configure o acesso Basic do professor para a entrega.
3. Confirme disponibilidade/minutos do agente Linux. O YAML usa `ubuntu-24.04`; a existência do pool Azure Pipelines não comprova concessão de execução gratuita. Não compre paralelismo automaticamente. Um agente próprio exige revisar o pool e instalar as ferramentas equivalentes.
4. Crie uma conexão de serviço **Azure Resource Manager** chamada `clyvocare-azure`, preferencialmente com identidade federada, limitada ao grupo dedicado, e autorize esta pipeline. Se usar outro nome, ajuste `azureServiceConnection` no YAML.
5. Conecte a pipeline ao repositório GitHub e selecione `/azure-pipelines.yml`. O YAML já define `azureResourceGroup: rm561584`; configure a variável não secreta `javaWebAppName` com o nome do Web App provisionado. Para outro ambiente, ajuste o grupo no YAML. Credenciais de Oracle, Atlas e RSA pertencem às configurações protegidas dos Web Apps; o CI não precisa delas. Se usar Azure DevOps para provisionar, mantenha esses valores como variáveis secretas/Secure Files e não como texto no YAML.

A .NET tem sua própria pipeline no repositório correspondente, apontando para o mesmo grupo e o seu Web App. O template compartilhado não cria o projeto DevOps, a conexão de serviço, a concessão do agente ou os acessos do professor. No ambiente RM561584, o projeto, as conexões e as duas pipelines já foram configurados, com CI/CD automático aprovado. Em outra instalação, confira novamente as permissões e execute o fluxo completo; a presença dos arquivos não comprova a entrega.

Para verificar os parâmetros e o fluxo do provisionamento sem criar recursos:

```bash
node scripts/appservice-parameters.test.cjs
bash scripts/provision-appservice.test.sh
```

Os testes usam credenciais/chaves temporárias e uma CLI simulada; conferem falhas antes de alterações, proteção de grupo alheio, reutilização do grupo próprio, limpeza dos segredos e falha da implantação. Não comprovam quota, preço, permissões ou aceitação do template pela Azure. Referências: [artefatos de pipelines](https://learn.microsoft.com/en-us/azure/devops/pipelines/artifacts/pipeline-artifacts), [deploy de ZIP](https://learn.microsoft.com/en-us/azure/app-service/deploy-zip) e [parâmetros seguros ARM](https://learn.microsoft.com/en-us/azure/azure-resource-manager/templates/parameters#secure-parameters).

### Conferindo a entrega de DevOps

Use a API publicada por HTTPS e a coleção Insomnia para demonstrar o CRUD completo de **pets e contratações**, com pelo menos dois registros significativos em cada tabela. As duas tabelas fazem parte do negócio e estão relacionadas pelo pet da contratação. Exclua as contratações antes dos pets usados no teste.

Conecte o SQL Developer ao mesmo Oracle FIAP e schema configurados nas APIs. Após cada operação, confira os registros:

```sql
SELECT * FROM TB_CAD_PET;
SELECT * FROM TB_CAD_SUBSCRIPTION;
```

O DDL comentado está em [script_bd.sql](script_bd.sql). Ele contém comandos de exclusão de tabelas e não deve ser executado para demonstrar essas consultas. Mostre no vídeo o provisionamento, uma alteração real no código na master, CI, resultados de testes, artefato, CD automático, recursos no portal e CRUD com `SELECT`, em resolução mínima de 720p e com narração. Confira os dados exigidos no PDF da respectiva sprint e o acesso do professor ao projeto e ao vídeo.

O encerramento do ambiente será feito somente após a correção, conferindo os recursos do grupo dedicado `rm561584`. Parar os Web Apps não encerra a cobrança do plano. Não existe exclusão automática no provisionamento ou nas pipelines.

## Tecnologias utilizadas

| Ferramenta | Papel |
|---|---|
| Java 17 e Spring Boot 4.0.6 | Linguagem e estrutura da aplicação |
| Spring Web MVC | Rotas HTTP |
| Spring Data JPA e Hibernate | Persistência no Oracle |
| Flyway e módulo Oracle | Versionamento do banco |
| Spring Security e OAuth2 Resource Server | Login e validação de JWT |
| BCrypt | Hash das senhas |
| Bean Validation | Validação dos campos de entrada |
| Spring Cache | Cache de consultas de estados e cidades |
| SpringDoc OpenAPI 3.0.2 | Documentação Swagger |
| Lombok | Geração de construtores, acessores e builders |
| Maven | Dependências e compilação |
| Azure App Service | Hospedagem nativa das APIs |
| Azure CLI e ARM | Provisionamento do plano, Web Apps e configurações |
| Azure DevOps e YAML | CI/CD, testes e artefatos |

As versões e dependências estão em [pom.xml](pom.xml). Embora existam dependências de H2 no arquivo, a configuração de execução usa Oracle.

## Estrutura e decisões de implementação

O código está organizado por camada. Para acompanhar uma requisição, comece no controller, siga para o service e depois para o repository:

```text
src/main/
├── java/br/com/fiap/ClyvoCareAPI/
│   ├── auth/          login, emissão de JWT e regras de segurança
│   ├── config/        CORS, OpenAPI e carga alternativa sem Flyway
│   ├── controller/    endpoints e respostas HTTP
│   ├── service/       regras de negócio e transações
│   ├── repository/    consultas e persistência com JPA
│   ├── entity/        mapeamento das tabelas e enums
│   ├── dto/           contratos de entrada e saída
│   └── validation/    tratamento dos erros de validação
└── resources/
    ├── application.properties
    └── db/migration/  versionamento do banco
```

Os DTOs são records que separam o JSON das entidades. Os controllers recebem os DTOs de entrada, chamam os serviços e convertem os resultados em DTOs de resposta. Os serviços recebem as requisições tipadas e trabalham com entidades para aplicar as regras e persistir os dados.

A lógica de preço fica em `ContractPricingService`, compartilhada pela simulação, criação, edição e troca de plano. As transições de status ficam em `SubscriptionLifecycleService`. Isso mantém o cálculo e as transições no mesmo lugar para todas essas operações.

As alterações de contratação usam transações e bloqueios dos registros envolvidos. A verificação de outra contratação ativa é feita com o pet bloqueado, evitando que duas operações concorrentes criem ou ativem contratos para ele ao mesmo tempo.

Estados e cidades têm cache nas consultas, invalidado nas alterações. As chaves estrangeiras continuam protegidas pelo Oracle, e os serviços também consultam os registros relacionados para devolver `404` quando uma referência não existe.

O `ValidationHandler` devolve erros de campos como uma lista de objetos `{field, message}`. Para `ResponseStatusException`, retorna `{status, message}`. Por isso, ao tratar erros no frontend, você precisa considerar os dois formatos.

O modelo Oracle compartilhado mantém a referência entre as clínicas da API .NET e as cidades cadastradas pela API Java. As APIs publicadas no App Service acessam o mesmo schema no Oracle da FIAP, mantendo os registros compartilhados.

Outros arquivos úteis:

| Arquivo | Conteúdo |
|---|---|
| [docs/clyvo-care-api.yaml](docs/clyvo-care-api.yaml) | Coleção Insomnia |
| [docs/MER.png](docs/MER.png) | Diagrama do banco |
| [script_bd.sql](script_bd.sql) | Script completo da entrega de Database |
| [scripts/start-appservice.sh](scripts/start-appservice.sh) | Inicialização nativa da Java no App Service |
| [scripts/provision-appservice.sh](scripts/provision-appservice.sh) | Provisionamento compartilhado por Azure CLI |
| [infra/appservice.json](infra/appservice.json) | Plano B2, dois Web Apps e configurações protegidas |
| [azure-pipelines.yml](azure-pipelines.yml) | CI/CD da API Java no Azure DevOps |

## Relação com a Sprint 3

| Disciplina | Onde encontrar a implementação |
|---|---|
| Java Advanced — frontend | Camada de visualização [ClyvoCare Web](https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1-Web), em React/Vite: login, pets, contratação, meus planos e gestão de status |
| Java Advanced — Flyway | Migrations V1 a V5 na execução normal |
| Java Advanced — Spring Security | Perfis `ADMIN` e `OWNER`, JWT e permissões nos endpoints |
| Java Advanced — fluxos de negócio | Simulação e contratação; mudança de status e troca de plano |
| DevOps | App Service, provisionamento por CLI e pipeline YAML com testes, artefato e CD |
| Database | Modelagem, scripts SQL, funções, procedimentos e trigger |
| .NET | [API complementar](https://github.com/ClyvoPet-Challenge-2026/.Net-Sprint-1), com clínicas, consultas de localização, observabilidade e projetos de testes |
| Mobile Application Development | Versão mobile das entregas no repositório [Clyvo-Care](https://github.com/ClyvoPet-Challenge-2026/Clyvo-Care) |

Para demonstrar o versionamento do banco, mantenha o Flyway habilitado. A verificação completa da entrega também depende de executar os fluxos publicados e apresentar as evidências exigidas pela disciplina.
