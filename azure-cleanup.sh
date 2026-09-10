#!/bin/bash
# ==============================================================================
# Script de limpeza de recursos na Azure para economizar créditos de estudante
# ==============================================================================
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-clyvocare-sprint3}"

echo "Removendo Resource Group $RESOURCE_GROUP e todos os recursos criados..."
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
echo "Exclusão iniciada em segundo plano na Azure."
