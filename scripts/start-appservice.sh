#!/usr/bin/env bash
set -euo pipefail

for variable in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD JWT_PRIVATE_KEY_BASE64 JWT_PUBLIC_KEY_BASE64; do
    if [[ -z "${!variable:-}" ]]; then
        echo "Missing required app setting: $variable" >&2
        exit 1
    fi
done

jar_path="${AZURE_JAVA_APP_PATH:-/home/site/wwwroot/app.jar}"
if [[ ! -f "$jar_path" ]]; then
    echo "Application JAR not found: $jar_path" >&2
    exit 1
fi
for executable in java openssl base64 cmp; do
    command -v "$executable" >/dev/null || { echo "Required executable not found: $executable" >&2; exit 1; }
done

umask 077
key_directory=$(mktemp -d "${TMPDIR:-/tmp}/clyvocare-jwt.XXXXXX")
trap 'rm -f -- "$key_directory/private_key.pem" "$key_directory/public_key.pem" "$key_directory/derived.pem" "$key_directory/normalized.pem"; rmdir -- "$key_directory"' EXIT
if ! printf '%s' "$JWT_PRIVATE_KEY_BASE64" | base64 --decode > "$key_directory/private_key.pem" \
    || ! printf '%s' "$JWT_PUBLIC_KEY_BASE64" | base64 --decode > "$key_directory/public_key.pem"; then
    echo "RSA app settings must contain Base64-encoded PEM files." >&2
    exit 1
fi
if ! openssl rsa -in "$key_directory/private_key.pem" -passin pass: -pubout -out "$key_directory/derived.pem" 2>/dev/null \
    || ! openssl rsa -pubin -in "$key_directory/public_key.pem" -pubout -out "$key_directory/normalized.pem" 2>/dev/null \
    || ! cmp -s -- "$key_directory/derived.pem" "$key_directory/normalized.pem"; then
    echo "RSA app settings must contain a valid matching key pair." >&2
    exit 1
fi
rm -- "$key_directory/derived.pem" "$key_directory/normalized.pem"

export JWT_PRIVATE_KEY="file:$key_directory/private_key.pem"
export JWT_PUBLIC_KEY="file:$key_directory/public_key.pem"
unset JWT_PRIVATE_KEY_BASE64 JWT_PUBLIC_KEY_BASE64
export SPRING_PROFILES_ACTIVE=appservice
# Let the JVM parse quoted options without evaluating shell commands.
if [[ -n "${JAVA_OPTS:-}" ]]; then
    export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:-} $JAVA_OPTS"
fi
exec java -jar "$jar_path"
