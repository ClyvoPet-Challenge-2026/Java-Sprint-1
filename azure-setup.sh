#!/bin/bash
set -e

# ========== VARIÁVEIS ==========
RESOURCE_GROUP="sprint1-javaapi-clyvo"
LOCATION="eastus2"
VM_NAME="vm-clyvo"
VM_SIZE="Standard_D2s_v3"
ADMIN_USER="admclyvo"
IMAGE="Canonical:ubuntu-24_04-lts:server:latest"

echo "=== 1. Criando Resource Group ==="
az group create --name $RESOURCE_GROUP --location $LOCATION

echo "=== 2. Criando VM Linux ==="
az vm create \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --image $IMAGE \
  --size $VM_SIZE \
  --location $LOCATION \
  --admin-username $ADMIN_USER \
  --generate-ssh-keys \
  --public-ip-sku Standard

echo "=== 3. Abrindo portas necessárias ==="
# Porta 22 (SSH) já é aberta automaticamente pelo Azure com prioridade 1000
# Abrimos apenas a 8080 para a API
az vm open-port --resource-group $RESOURCE_GROUP --name $VM_NAME --port 8080 --priority 900

echo "=== 4. Instalando Docker na VM ==="
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $ADMIN_USER
    sudo systemctl enable docker
    sudo systemctl start docker
  "

echo "=== 5. Instalando Git e ferramentas ==="
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    sudo apt-get update -y
    sudo apt-get install -y git nano curl wget unzip
    docker --version
    git --version
  "

echo "=== 6. Clonando repositório e criando docker-compose.yml ==="
az vm run-command invoke \
  --resource-group $RESOURCE_GROUP \
  --name $VM_NAME \
  --command-id RunShellScript \
  --scripts "
    git clone https://github.com/ClyvoPet-Challenge-2026/Java-Sprint-1.git /home/$ADMIN_USER/Java-Sprint-1

    cat > /home/$ADMIN_USER/Java-Sprint-1/docker-compose.yml << 'EOF'
services:
  oracle-db:
    image: gvenzl/oracle-xe:21-slim
    container_name: clyvocare-oracle
    environment:
      ORACLE_PASSWORD: Oracle_123
      APP_USER: clyvocare
      APP_USER_PASSWORD: clyvo123
    ports:
      - \"1521:1521\"
    volumes:
      - oracle-data:/opt/oracle/oradata
    healthcheck:
      test: [\"CMD\", \"healthcheck.sh\"]
      interval: 30s
      timeout: 10s
      retries: 10
  api:
    build: .
    container_name: clyvocare-api
    ports:
      - \"8080:8080\"
    environment:
      SPRING_DATASOURCE_URL: jdbc:oracle:thin:@oracle-db:1521/XEPDB1
      SPRING_DATASOURCE_USERNAME: clyvocare
      SPRING_DATASOURCE_PASSWORD: clyvo123
    depends_on:
      oracle-db:
        condition: service_healthy
    user: \"1000\"
volumes:
  oracle-data:
EOF

    cd /home/$ADMIN_USER/Java-Sprint-1
    docker compose up --build -d
  "

# Captura IP público
VM_IP=$(az vm show -d --resource-group $RESOURCE_GROUP --name $VM_NAME --query publicIps -o tsv)
echo ""
echo "=== CONCLUÍDO ==="
echo "IP da VM: $VM_IP"
echo "Swagger: http://$VM_IP:8080/swagger-ui.html"
echo "Conecte via: ssh $ADMIN_USER@$VM_IP"
