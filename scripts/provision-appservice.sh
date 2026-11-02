#!/usr/bin/env bash
set -euo pipefail

for variable in AZURE_SUBSCRIPTION_ID AZURE_RESOURCE_GROUP AZURE_PLAN_NAME JAVA_WEBAPP_NAME DOTNET_WEBAPP_NAME; do
    if [[ -z "${!variable:-}" ]]; then
        echo "Missing required environment variable: $variable" >&2
        exit 1
    fi
done
for executable in az node; do
    command -v "$executable" >/dev/null || { echo "Required executable not found: $executable" >&2; exit 1; }
done
repository_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
umask 077
parameter_file=$(mktemp)
trap 'rm -f -- "$parameter_file"' EXIT
node "$repository_directory/scripts/appservice-parameters.cjs" > "$parameter_file"

# Preserve Azure resource IDs and Linux startup paths when using Git Bash.
export MSYS_NO_PATHCONV=1
template_path="$repository_directory/infra/appservice.json"
parameter_path="$parameter_file"
if command -v cygpath >/dev/null; then
    template_path=$(cygpath -m "$template_path")
    parameter_path=$(cygpath -m "$parameter_path")
fi
az account show --subscription "$AZURE_SUBSCRIPTION_ID" --output none
group_exists=$(az group exists --subscription "$AZURE_SUBSCRIPTION_ID" --name "$AZURE_RESOURCE_GROUP" --output tsv)
if [[ "$group_exists" == true ]]; then
    project=$(az group show --subscription "$AZURE_SUBSCRIPTION_ID" --name "$AZURE_RESOURCE_GROUP" --query tags.project --output tsv)
    if [[ "$project" != clyvocare-sprint4 ]]; then
        echo "Refusing to change a resource group not tagged project=clyvocare-sprint4." >&2
        exit 1
    fi
elif [[ "$group_exists" == false ]]; then
    az group create --subscription "$AZURE_SUBSCRIPTION_ID" --name "$AZURE_RESOURCE_GROUP" \
        --location "${AZURE_LOCATION:-eastus}" --tags project=clyvocare-sprint4 --output none
else
    echo "Unable to determine whether the resource group exists." >&2
    exit 1
fi
az provider register --subscription "$AZURE_SUBSCRIPTION_ID" --namespace Microsoft.Web --wait --output none
az deployment group create --subscription "$AZURE_SUBSCRIPTION_ID" --resource-group "$AZURE_RESOURCE_GROUP" \
    --name clyvocare-appservice --mode Incremental --template-file "$template_path" \
    --parameters "@$parameter_path" --output none
az deployment group show --subscription "$AZURE_SUBSCRIPTION_ID" --resource-group "$AZURE_RESOURCE_GROUP" \
    --name clyvocare-appservice --query properties.outputs --output json
