#!/bin/bash
set -e

# ==============================================================================
# ClyvoCare API - Script de Automação Azure (ACR + ACI)
# Disciplina: DevOps Tools & Cloud Computing - Sprint 3
# Região: mexicocentral (Otimizado para Assinatura de Estudantes)
# ==============================================================================

GREEN='[0;32m'
CYAN='[0;36m'
YELLOW='[1;33m'
RED='[0;31m'
NC='[0m'

echo -e "${CYAN}=====================================================${NC}"
echo -e "${CYAN}   CLYVOCARE API - PROVISIONAMENTO ACR + ACI AZURE   ${NC}"
echo -e "${CYAN}   Região: mexicocentral (Azure for Students)        ${NC}"
echo -e "${CYAN}=====================================================${NC}"

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-clyvocare-sprint3}"
LOCATION="${LOCATION:-mexicocentral}"
RANDOM_SUFFIX=$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 5 | head -n 1)
ACR_NAME="${ACR_NAME:-acrclyvo${RANDOM_SUFFIX}}"
CONTAINER_GROUP_NAME="${CONTAINER_GROUP_NAME:-aci-clyvocare-group}"
DNS_LABEL="${DNS_LABEL:-clyvocare-api-${RANDOM_SUFFIX}}"

ORACLE_PWD="${ORACLE_PWD:-Oracle_123}"
APP_DB_USER="${APP_DB_USER:-clyvocare}"
APP_DB_PWD="${APP_DB_PWD:-clyvo123}"

echo -e "
${YELLOW}[INFO] Parâmetros de Execução:${NC}"
echo -e "  - Resource Group:         ${GREEN}$RESOURCE_GROUP${NC}"
echo -e "  - Região (Location):       ${GREEN}$LOCATION${NC}"
echo -e "  - Nome do ACR:            ${GREEN}$ACR_NAME${NC}"
echo -e "  - Container Group (ACI):  ${GREEN}$CONTAINER_GROUP_NAME${NC}"
echo -e "  - DNS Label / FQDN:       ${GREEN}$DNS_LABEL.$LOCATION.azurecontainer.io${NC}"

echo -e "
${YELLOW}=== 1. Verificando Autenticação Azure CLI ===${NC}"
if ! az account show > /dev/null 2>&1; then
    echo -e "${RED}[ERRO] Você não está autenticado no Azure CLI. Execute 'az login' antes de rodar este script.${NC}"
    exit 1
fi

SUBSCRIPTION_NAME=$(az account show --query name -o tsv)
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo -e "Assinatura Ativa: ${GREEN}$SUBSCRIPTION_NAME ($SUBSCRIPTION_ID)${NC}"

echo -e "
${YELLOW}=== 2. Criando Resource Group ($RESOURCE_GROUP na região $LOCATION) ===${NC}"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output table

echo -e "
${YELLOW}=== 3. Criando Azure Container Registry (SKU Basic - Estudante) ===${NC}"
az acr create     --resource-group "$RESOURCE_GROUP"     --name "$ACR_NAME"     --sku Basic     --admin-enabled true     --location "$LOCATION"     --output table

echo -e "\n${YELLOW}=== 4. Obtendo Credenciais e Autenticando no ACR ===${NC}"
ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --resource-group "$RESOURCE_GROUP" --query loginServer -o tsv)
ACR_USERNAME=$(az acr credential show --name "$ACR_NAME" --resource-group "$RESOURCE_GROUP" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --resource-group "$RESOURCE_GROUP" --query "passwords[0].value" -o tsv)

echo "$ACR_PASSWORD" | docker login "$ACR_LOGIN_SERVER" -u "$ACR_USERNAME" --password-stdin

echo -e "\n${YELLOW}=== 5. Efetuando Build & Push da imagem da API via Docker ===${NC}"
echo -e "Construindo imagem multi-stage localmente: $ACR_LOGIN_SERVER/clyvocare-api:v1 ..."
docker build -t "$ACR_LOGIN_SERVER/clyvocare-api:v1" .

echo -e "Enviando imagem para o Azure Container Registry ($ACR_LOGIN_SERVER)..."
docker push "$ACR_LOGIN_SERVER/clyvocare-api:v1"

echo -e "
${YELLOW}=== 6. Gerando manifesto declarativo para o Container Group (Multi-Container: App + Oracle) ===${NC}"
ACI_YAML_FILE="aci-deployment.generated.yaml"

cat <<EOF > "$ACI_YAML_FILE"
apiVersion: 2021-10-01
location: $LOCATION
name: $CONTAINER_GROUP_NAME
properties:
  osType: Linux
  restartPolicy: Always
  imageRegistryCredentials:
    - server: $ACR_LOGIN_SERVER
      username: $ACR_USERNAME
      password: $ACR_PASSWORD
  ipAddress:
    type: Public
    dnsNameLabel: $DNS_LABEL
    ports:
      - protocol: tcp
        port: 8080
      - protocol: tcp
        port: 1521
  containers:
    - name: oracle-db
      properties:
        image: gvenzl/oracle-xe:21-slim
        resources:
          requests:
            cpu: 1.0
            memoryInGB: 2.5
        environmentVariables:
          - name: ORACLE_PASSWORD
            value: "$ORACLE_PWD"
          - name: APP_USER
            value: "$APP_DB_USER"
          - name: APP_USER_PASSWORD
            value: "$APP_DB_PWD"
        ports:
          - port: 1521
    - name: clyvocare-api
      properties:
        image: $ACR_LOGIN_SERVER/clyvocare-api:v1
        resources:
          requests:
            cpu: 1.0
            memoryInGB: 1.5
        environmentVariables:
          - name: SPRING_DATASOURCE_URL
            value: "jdbc:oracle:thin:@127.0.0.1:1521/XEPDB1"
          - name: SPRING_DATASOURCE_USERNAME
            value: "system"
          - name: SPRING_DATASOURCE_PASSWORD
            value: "$ORACLE_PWD"
        ports:
          - port: 8080
EOF

echo -e "
${YELLOW}=== 7. Provisionando Container Group no ACI ($CONTAINER_GROUP_NAME) ===${NC}"
az container create     --resource-group "$RESOURCE_GROUP"     --file "$ACI_YAML_FILE"

echo -e "
${YELLOW}=== 8. Obtendo informações de rede do ACI ===${NC}"
ACI_IP=$(az container show --resource-group "$RESOURCE_GROUP" --name "$CONTAINER_GROUP_NAME" --query ipAddress.ip -o tsv)
ACI_FQDN=$(az container show --resource-group "$RESOURCE_GROUP" --name "$CONTAINER_GROUP_NAME" --query ipAddress.fqdn -o tsv)

echo -e "
${GREEN}======================================================================${NC}"
echo -e "${GREEN}   DEPLOY CONCLUÍDO COM SUCESSO NO AZURE ACR + ACI (mexicocentral)    ${NC}"
echo -e "${GREEN}======================================================================${NC}"
echo -e "IP Público ACI:        ${CYAN}http://$ACI_IP:8080${NC}"
echo -e "FQDN ACI:              ${CYAN}http://$ACI_FQDN:8080${NC}"
echo -e "Documentação Swagger:  ${CYAN}http://$ACI_FQDN:8080/swagger-ui.html${NC}"
echo -e "Endpoint OpenAPI JSON: ${CYAN}http://$ACI_FQDN:8080/v3/api-docs${NC}"
echo -e "Conexão Banco Oracle:  ${CYAN}$ACI_IP:1521/XEPDB1 (User: $APP_DB_USER)${NC}"
echo -e "
Para visualizar logs do App:"
echo -e "  ${YELLOW}az container logs --resource-group $RESOURCE_GROUP --name $CONTAINER_GROUP_NAME --container-name clyvocare-api --follow${NC}"
echo -e "
Para visualizar logs do Oracle DB:"
echo -e "  ${YELLOW}az container logs --resource-group $RESOURCE_GROUP --name $CONTAINER_GROUP_NAME --container-name oracle-db --follow${NC}"
echo -e "======================================================================
"
