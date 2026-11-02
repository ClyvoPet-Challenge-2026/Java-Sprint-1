#!/usr/bin/env bash
set -euo pipefail

script_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
test_directory=$(mktemp -d)
export TEST_CALLS="$test_directory/calls"
export TEST_PARAMETER_PATH="$test_directory/parameter-path"
trap 'rm -f -- "$test_directory/private_key.pem" "$test_directory/public_key.pem" "$TEST_CALLS" "$TEST_PARAMETER_PATH" "$test_directory/output"; rmdir -- "$test_directory"' EXIT
trap 'echo "Provisioning test failed at line $LINENO." >&2; cat "$test_directory/output" >&2' ERR
bash "$script_directory/generate-keys.sh" "$test_directory" > /dev/null
export AZURE_SUBSCRIPTION_ID=00000000-0000-0000-0000-000000000001
export AZURE_RESOURCE_GROUP=rg-clyvocare-test
export AZURE_PLAN_NAME=asp-clyvocare-test
export JAVA_WEBAPP_NAME=clyvocare-java-test
export DOTNET_WEBAPP_NAME=clyvocare-dotnet-test
export SPRING_DATASOURCE_URL='jdbc:oracle:thin:@example.invalid:1521:ORCL'
export SPRING_DATASOURCE_USERNAME=test
export SPRING_DATASOURCE_PASSWORD=test
export ConnectionStrings__ClyvoCareOracle='Data Source=example.invalid:1521/ORCL;User Id=test;Password=test;'
export Mongo__ConnectionString='mongodb://example.invalid:27017'
export Mongo__DatabaseName=test
export JWT_PRIVATE_KEY_BASE64
export JWT_PUBLIC_KEY_BASE64
JWT_PRIVATE_KEY_BASE64=$(base64 < "$test_directory/private_key.pem" | tr -d '\r\n')
JWT_PUBLIC_KEY_BASE64=$(base64 < "$test_directory/public_key.pem" | tr -d '\r\n')

az() {
    printf '%s\n' "$*" >> "$TEST_CALLS"
    case "$1 $2 ${3:-}" in
        'account show '*) return "${TEST_ACCOUNT_EXIT:-0}" ;;
        'group exists '*) echo "${TEST_GROUP_EXISTS:-false}"; return "${TEST_EXISTS_EXIT:-0}" ;;
        'group show '*) echo "${TEST_PROJECT_TAG:-clyvocare-sprint4}" ;;
        'group create '*) return 0 ;;
        'provider register '*) return 0 ;;
        'deployment group create')
            local argument parameter_path=''
            for argument in "$@"; do
                if [[ "$argument" == @* ]]; then parameter_path="${argument#@}"; fi
            done
            [[ -f "$parameter_path" ]]
            printf '%s' "$parameter_path" > "$TEST_PARAMETER_PATH"
            if [[ "$(uname -s)" == Linux ]]; then [[ "$(stat -c %a "$parameter_path")" == 600 ]]; fi
            return "${TEST_DEPLOY_EXIT:-0}"
            ;;
        'deployment group show') echo '{}' ;;
        *) echo 'Unexpected Azure command.' >&2; return 1 ;;
    esac
}
export -f az

expect_failure() {
    : > "$TEST_CALLS"
    if "$@" > "$test_directory/output" 2>&1; then
        echo 'Unsafe provisioning unexpectedly succeeded.' >&2
        exit 1
    fi
    if grep -Eq '^group create|^deployment group create' "$TEST_CALLS"; then
        echo 'Invalid configuration reached a cloud mutation.' >&2
        exit 1
    fi
}
expect_failure env -u AZURE_SUBSCRIPTION_ID bash "$script_directory/provision-appservice.sh"
expect_failure env -u Mongo__ConnectionString bash "$script_directory/provision-appservice.sh"
expect_failure env TEST_ACCOUNT_EXIT=1 bash "$script_directory/provision-appservice.sh"
expect_failure env TEST_EXISTS_EXIT=1 bash "$script_directory/provision-appservice.sh"
expect_failure env TEST_GROUP_EXISTS=unknown bash "$script_directory/provision-appservice.sh"
expect_failure env TEST_GROUP_EXISTS=true TEST_PROJECT_TAG=unrelated bash "$script_directory/provision-appservice.sh"

for existing in false true; do
    : > "$TEST_CALLS"
    env TEST_GROUP_EXISTS="$existing" bash "$script_directory/provision-appservice.sh" > "$test_directory/output" 2>&1
    grep -q '^deployment group create.*--mode Incremental' "$TEST_CALLS"
    [[ ! -f "$(cat "$TEST_PARAMETER_PATH")" ]]
    if [[ "$existing" == true ]]; then ! grep -q '^group create' "$TEST_CALLS"; fi
    if [[ "$existing" == false ]]; then grep -q '^group create.*project=clyvocare-sprint4' "$TEST_CALLS"; fi
done
if env TEST_DEPLOY_EXIT=17 bash "$script_directory/provision-appservice.sh" > "$test_directory/output" 2>&1; then
    echo 'Deployment failure was not propagated.' >&2
    exit 1
else
    [[ "$?" == 17 && ! -f "$(cat "$TEST_PARAMETER_PATH")" ]]
fi
echo 'App Service provisioning: assertions passed.'
