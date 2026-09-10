#!/bin/bash
# ==============================================================================
# ClyvoCare API - Script de Testes Automatizados no Azure ACR + ACI
# Disciplina: DevOps Tools & Cloud Computing - Sprint 3
# ==============================================================================

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-clyvocare-sprint3}"
CONTAINER_GROUP_NAME="${CONTAINER_GROUP_NAME:-aci-clyvocare-group}"
API_URL="${1:-$API_URL}"

echo -e "${CYAN}=====================================================${NC}"
echo -e "${CYAN}   CLYVOCARE API - SUÍTE DE TESTES (AZURE ACI)       ${NC}"
echo -e "${CYAN}=====================================================${NC}"

# Se API_URL não foi informada como parâmetro ou variável, tenta obter via Azure CLI
if [ -z "$API_URL" ]; then
    echo -e "${YELLOW}[INFO] Buscando endpoint público do Container Group no Azure...${NC}"
    if az account show > /dev/null 2>&1; then
        ACI_FQDN=$(az container show --resource-group "$RESOURCE_GROUP" --name "$CONTAINER_GROUP_NAME" --query ipAddress.fqdn -o tsv 2>/dev/null || true)
        ACI_IP=$(az container show --resource-group "$RESOURCE_GROUP" --name "$CONTAINER_GROUP_NAME" --query ipAddress.ip -o tsv 2>/dev/null || true)

        if [ -n "$ACI_FQDN" ]; then
            API_URL="http://$ACI_FQDN:8080"
        elif [ -n "$ACI_IP" ]; then
            API_URL="http://$ACI_IP:8080"
        fi
    fi
fi

# Se ainda estiver vazio, solicita fallback interativo ou localhost
if [ -z "$API_URL" ]; then
    echo -e "${YELLOW}[AVISO] Não foi possível obter o IP/FQDN do ACI automaticamente.${NC}"
    read -p "Informe a URL da API (ex: http://clyvocare-api.mexicocentral.azurecontainer.io:8080): " API_URL
    API_URL="${API_URL:-http://localhost:8080}"
fi

# Remove barra final se houver
API_URL="${API_URL%/}"

echo -e "Target Base URL: ${GREEN}$API_URL${NC}"
echo -e "Resource Group:  ${GREEN}$RESOURCE_GROUP${NC}"
echo -e "Container Group: ${GREEN}$CONTAINER_GROUP_NAME${NC}"
echo -e "-----------------------------------------------------"

TESTS_PASSED=0
TESTS_FAILED=0

assert_status() {
    local step_name="$1"
    local expected="$2"
    local actual="$3"
    local response_body="$4"

    if [ "$actual" -eq "$expected" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $step_name (HTTP $actual)"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} $step_name (Esperado HTTP $expected, Retornou HTTP $actual)"
        if [ -n "$response_body" ]; then
            echo -e "  ${RED}Detalhes do erro:${NC} $response_body"
        fi
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# ------------------------------------------------------------------------------
# 1. VERIFICAÇÃO DO STATUS DOS CONTAINERS NO AZURE (se az CLI disponível)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}=== 1. Verificando Status no Azure ACI ===${NC}"
if az account show > /dev/null 2>&1; then
    CONTAINER_STATE=$(az container show --resource-group "$RESOURCE_GROUP" --name "$CONTAINER_GROUP_NAME" --query "instanceView.state" -o tsv 2>/dev/null || echo "UNKNOWN")
    echo -e "Estado do Container Group: ${BOLD}$CONTAINER_STATE${NC}"
    
    if [ "$CONTAINER_STATE" == "Running" ]; then
        echo -e "  ${GREEN}[PASS]${NC} Containers em execução no Azure."
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "  ${YELLOW}[AVISO]${NC} Container Group não está no estado 'Running' ou não foi encontrado."
    fi
else
    echo -e "Azure CLI não autenticado no momento. Pulando checagem direta do Azure CLI."
fi

# ------------------------------------------------------------------------------
# 2. HEALTH / OPENAPI & SWAGGER UI
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}=== 2. Testando Documentação OpenAPI / Swagger ===${NC}"

SWAGGER_CODE=$(curl -s -L -o /dev/null -w "%{http_code}" "$API_URL/swagger-ui.html" || echo "000")
assert_status "Acesso ao Swagger UI (/swagger-ui.html)" 200 "$SWAGGER_CODE"

OPENAPI_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/v3/api-docs" || echo "000")
assert_status "Acesso ao JSON da OpenAPI (/v3/api-docs)" 200 "$OPENAPI_CODE"

# ------------------------------------------------------------------------------
# 3. AUTENTICAÇÃO (LOGIN JWT COM ADMIN)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}=== 3. Testando Autenticação (POST /auth/login) ===${NC}"

LOGIN_PAYLOAD='{"email":"ana@email.com","password":"senha123"}'
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_PAYLOAD")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

assert_status "Login com usuário administrador (ana@email.com)" 200 "$HTTP_CODE" "$BODY"

TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"token": "[^"]*' | cut -d'"' -f4)
fi

if [ -n "$TOKEN" ]; then
    echo -e "  ${GREEN}[INFO]${NC} JWT Token obtido com sucesso! (${TOKEN:0:20}...)"
else
    echo -e "  ${RED}[ERRO] Não foi possível extrair o token JWT do login.${NC}"
fi

# ------------------------------------------------------------------------------
# 4. CONSULTAS DE DOMÍNIO / TABELAS DE APOIO (PROTEGIDAS)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}=== 4. Testando Endpoints de Consulta (com Bearer Token) ===${NC}"

# Planos
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/planos" -H "Authorization: Bearer $TOKEN")
assert_status "GET /planos" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# Espécies
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/especies" -H "Authorization: Bearer $TOKEN")
assert_status "GET /especies" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# Raças
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/racas" -H "Authorization: Bearer $TOKEN")
assert_status "GET /racas" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# Estados
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/estados" -H "Authorization: Bearer $TOKEN")
assert_status "GET /estados" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# Cidades
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/cidades" -H "Authorization: Bearer $TOKEN")
assert_status "GET /cidades" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# Contratações / Assinaturas
RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/contratacoes" -H "Authorization: Bearer $TOKEN")
assert_status "GET /contratacoes" 200 "$(echo "$RESP" | tail -n1)" "$(echo "$RESP" | sed '$d')"

# ------------------------------------------------------------------------------
# 5. FLUXO END-TO-END (CADASTRO DE TUTOR + PET)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}=== 5. Testando Fluxo de Cadastro e Validação (POST /responsaveis e /pets) ===${NC}"

# Função para gerar CPF com dígito verificador válido
generate_cpf() {
    python3 -c "
import random
cpf = [random.randint(0, 9) for _ in range(9)]
d1 = sum(a * b for a, b in zip(cpf, range(10, 1, -1))) % 11
d1 = 0 if d1 < 2 else 11 - d1
cpf.append(d1)
d2 = sum(a * b for a, b in zip(cpf, range(11, 1, -1))) % 11
d2 = 0 if d2 < 2 else 11 - d2
cpf.append(d2)
print(f'{cpf[0]}{cpf[1]}{cpf[2]}.{cpf[3]}{cpf[4]}{cpf[5]}.{cpf[6]}{cpf[7]}{cpf[8]}-{cpf[9]}{cpf[10]}')
" 2>/dev/null || echo "123.456.789-09"
}

TEST_CPF=$(generate_cpf)
RAND_SUFFIX=$(cat /dev/urandom | tr -dc '0-9' | fold -w 6 | head -n 1)
TEST_EMAIL="tutor_${RAND_SUFFIX:-999999}@clyvocaretest.com"

OWNER_PAYLOAD=$(cat <<EOF
{
  "name": "Tutor Teste Automatizado",
  "cpf": "$TEST_CPF",
  "email": "$TEST_EMAIL",
  "password": "SenhaSegura123",
  "phone": "(11) 98765-4321",
  "cityId": 1
}
EOF
)

CREATE_OWNER_RESP=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/responsaveis" \
    -H "Content-Type: application/json" \
    -d "$OWNER_PAYLOAD")

OWNER_CODE=$(echo "$CREATE_OWNER_RESP" | tail -n1)
OWNER_BODY=$(echo "$CREATE_OWNER_RESP" | sed '$d')

assert_status "Cadastro de Novo Responsável (POST /responsaveis)" 201 "$OWNER_CODE" "$OWNER_BODY"

CREATED_OWNER_ID=$(echo "$OWNER_BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)

if [ -n "$CREATED_OWNER_ID" ]; then
    echo -e "  ${GREEN}[INFO]${NC} Responsável ID gerado: $CREATED_OWNER_ID"

    # Cadastro de Pet para o Tutor criado
    PET_PAYLOAD=$(cat <<EOF
{
  "name": "Pipoca Teste",
  "birthDate": "2023-01-15",
  "sex": "FEMALE",
  "ownerId": $CREATED_OWNER_ID,
  "speciesId": 1,
  "breedId": 1
}
EOF
    )

    PET_RESP=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/pets" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "$PET_PAYLOAD")
    PET_CODE=$(echo "$PET_RESP" | tail -n1)
    PET_BODY=$(echo "$PET_RESP" | sed '$d')
    assert_status "Cadastro de Pet vinculado ao Responsável (POST /pets)" 201 "$PET_CODE" "$PET_BODY"

    CREATED_PET_ID=$(echo "$PET_BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)
    
    if [ -n "$CREATED_PET_ID" ]; then
        # Consulta o pet criado
        GET_PET_RESP=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/pets/$CREATED_PET_ID" -H "Authorization: Bearer $TOKEN")
        assert_status "Consulta do Pet por ID (GET /pets/$CREATED_PET_ID)" 200 "$(echo "$GET_PET_RESP" | tail -n1)"
    fi
fi

# ------------------------------------------------------------------------------
# RELATÓRIO FINAL
# ------------------------------------------------------------------------------
echo -e "\n====================================================="
echo -e "                RESUMO DOS TESTES                    "
echo -e "====================================================="
echo -e "  Testes com Sucesso: ${GREEN}$TESTS_PASSED${NC}"
echo -e "  Testes com Falha:   ${RED}$TESTS_FAILED${NC}"
echo -e "====================================================="

if [ "$TESTS_FAILED" -eq 0 ]; then
    echo -e "${GREEN}🎉 TODOS OS TESTES PASSARAM COM SUCESSO NO AZURE ACI!${NC}\n"
    exit 0
else
    echo -e "${RED}⚠️  HOUVE FALHAS EM ALGUNS TESTES. VERIFIQUE OS LOGS ACIMA.${NC}\n"
    exit 1
fi
