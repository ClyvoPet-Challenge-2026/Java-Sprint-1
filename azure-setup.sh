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
az vm open-port --resource-group $RESOURCE_GROUP --name $VM_NAME --port 8080 --priority 900
az vm open-port --resource-group $RESOURCE_GROUP --name $VM_NAME --port 22 --priority 1000

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

# Captura IP público
VM_IP=$(az vm show -d --resource-group $RESOURCE_GROUP --name $VM_NAME --query publicIps -o tsv)
echo ""
echo "=== CONCLUÍDO ==="
echo "IP da VM: $VM_IP"
echo "Conecte via: ssh admclyvo@$VM_IP"
