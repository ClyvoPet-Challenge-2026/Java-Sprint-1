#!/usr/bin/env bash
set -euo pipefail

script_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
test_directory=$(mktemp -d)
key_directory="$test_directory/keys with spaces"
trap 'rm -f -- "$key_directory/private_key.pem" "$key_directory/public_key.pem" "$test_directory/expected.pem"; rmdir -- "$key_directory" "$test_directory"' EXIT

bash "$script_directory/generate-keys.sh" "$key_directory" >/dev/null
openssl pkey -in "$key_directory/private_key.pem" -check -noout >/dev/null
openssl pkey -in "$key_directory/private_key.pem" -pubout -out "$test_directory/expected.pem"
cmp -- "$test_directory/expected.pem" "$key_directory/public_key.pem"
if bash "$script_directory/generate-keys.sh" "$key_directory" >/dev/null 2>&1; then
    echo "Existing keys were not protected." >&2
    exit 1
fi
openssl pkey -in "$key_directory/private_key.pem" -pubout | cmp -- "$test_directory/expected.pem" -
rm -- "$key_directory/public_key.pem"
if bash "$script_directory/generate-keys.sh" "$key_directory" >/dev/null 2>&1; then
    echo "An incomplete existing key pair was not protected." >&2
    exit 1
fi
echo "RSA setup: assertions passed."
