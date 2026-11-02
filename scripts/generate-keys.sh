#!/usr/bin/env bash
set -euo pipefail

key_directory="${1:-$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)/.keys}"
command -v openssl >/dev/null || { echo "OpenSSL is required." >&2; exit 1; }
mkdir -p -- "$key_directory"
if [[ -e "$key_directory/private_key.pem" || -e "$key_directory/public_key.pem" ]]; then
    echo "Key files already exist; refusing to replace the RSA pair." >&2
    exit 1
fi

umask 077
private_key=$(mktemp "$key_directory/private.XXXXXX")
public_key=$(mktemp "$key_directory/public.XXXXXX")
trap 'rm -f -- "$private_key" "$public_key"' EXIT
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key" 2>/dev/null
openssl pkey -in "$private_key" -pubout -out "$public_key"
mv -- "$private_key" "$key_directory/private_key.pem"
mv -- "$public_key" "$key_directory/public_key.pem"
echo "RSA pair created in $key_directory. Share only public_key.pem with the .NET API."
