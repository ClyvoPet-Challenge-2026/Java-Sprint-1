#!/usr/bin/env bash
set -euo pipefail

script_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
test_directory=$(mktemp -d)
export TMPDIR="$test_directory/runtime"
mkdir -- "$TMPDIR" "$test_directory/bin"
cleanup() {
    local directory
    for directory in "$TMPDIR"/clyvocare-jwt.* "$test_directory/keys" "$test_directory/other-keys" "$test_directory/bin"; do
        if [[ -d "$directory" ]]; then
            rm -f -- "$directory"/*
            rmdir -- "$directory"
        fi
    done
    rm -f -- "$test_directory/app with spaces.jar" "$test_directory/output"
    rmdir -- "$TMPDIR" "$test_directory"
}
trap cleanup EXIT
trap 'echo "App Service startup test failed at line $LINENO." >&2; cat "$test_directory/output" >&2' ERR

bash "$script_directory/generate-keys.sh" "$test_directory/keys" >/dev/null
bash "$script_directory/generate-keys.sh" "$test_directory/other-keys" >/dev/null
export EXPECTED_KEY_DIRECTORY="$test_directory/keys"
export AZURE_JAVA_APP_PATH="$test_directory/app with spaces.jar"
touch -- "$AZURE_JAVA_APP_PATH"
export SPRING_DATASOURCE_URL='jdbc:oracle:thin:@example.invalid:1521/ORCL'
export SPRING_DATASOURCE_USERNAME=test
export SPRING_DATASOURCE_PASSWORD=test
export JWT_PRIVATE_KEY_BASE64
export JWT_PUBLIC_KEY_BASE64
JWT_PRIVATE_KEY_BASE64=$(base64 < "$test_directory/keys/private_key.pem" | tr -d '\r\n')
JWT_PUBLIC_KEY_BASE64=$(base64 < "$test_directory/keys/public_key.pem" | tr -d '\r\n')
export JAVA_OPTS='-Xmx768m -Dexample="value with spaces"'
export JDK_JAVA_OPTIONS='-Dfile.encoding=UTF-8'

cat > "$test_directory/bin/java" <<'JAVA'
#!/usr/bin/env bash
set -euo pipefail
trap 'echo "Java launch assertion failed at line $LINENO." >&2' ERR
[[ "$#" == 2 && "$1" == -jar && "$2" == "$AZURE_JAVA_APP_PATH" ]]
[[ "$SPRING_PROFILES_ACTIVE" == appservice ]]
[[ "$JDK_JAVA_OPTIONS" == '-Dfile.encoding=UTF-8 -Xmx768m -Dexample="value with spaces"' ]]
[[ -z "${JWT_PRIVATE_KEY_BASE64+x}" && -z "${JWT_PUBLIC_KEY_BASE64+x}" ]]
cmp -- "${JWT_PRIVATE_KEY#file:}" "$EXPECTED_KEY_DIRECTORY/private_key.pem"
cmp -- "${JWT_PUBLIC_KEY#file:}" "$EXPECTED_KEY_DIRECTORY/public_key.pem"
if [[ "$(uname -s)" == Linux ]]; then
    [[ "$(stat -c %a "${JWT_PRIVATE_KEY#file:}")" == 600 ]]
    [[ "$(stat -c %a "$(dirname -- "${JWT_PRIVATE_KEY#file:}")")" == 700 ]]
fi
echo 'Java launch assertions passed.'
exit "${JAVA_TEST_EXIT_CODE:-0}"
JAVA
chmod +x "$test_directory/bin/java"
export PATH="$test_directory/bin:$PATH"

expect_failure() {
    if "$@" > "$test_directory/output" 2>&1; then
        echo 'Invalid startup unexpectedly succeeded.' >&2
        exit 1
    fi
    if compgen -G "$TMPDIR/clyvocare-jwt.*" > /dev/null; then
        echo 'Failed startup left temporary keys behind.' >&2
        exit 1
    fi
}

expect_failure env -u SPRING_DATASOURCE_PASSWORD bash "$script_directory/start-appservice.sh"
expect_failure env -u JWT_PUBLIC_KEY_BASE64 bash "$script_directory/start-appservice.sh"
expect_failure env AZURE_JAVA_APP_PATH="$test_directory/missing.jar" bash "$script_directory/start-appservice.sh"
expect_failure env JWT_PRIVATE_KEY_BASE64='invalid!' bash "$script_directory/start-appservice.sh"
expect_failure env JWT_PUBLIC_KEY_BASE64='invalid!' bash "$script_directory/start-appservice.sh"
expect_failure env JWT_PRIVATE_KEY_BASE64="$(printf 'not a PEM' | base64)" bash "$script_directory/start-appservice.sh"
expect_failure env JWT_PUBLIC_KEY_BASE64="$(base64 < "$test_directory/other-keys/public_key.pem" | tr -d '\r\n')" bash "$script_directory/start-appservice.sh"

for attempt in 1 2; do
    bash "$script_directory/start-appservice.sh" > "$test_directory/output" 2>&1
    [[ "$(cat "$test_directory/output")" == 'Java launch assertions passed.' ]]
done
if env JAVA_TEST_EXIT_CODE=23 bash "$script_directory/start-appservice.sh" > "$test_directory/output" 2>&1; then
    echo 'Java failure was not propagated.' >&2
    exit 1
else
    [[ "$?" == 23 ]]
fi

echo 'App Service startup: assertions passed.'
