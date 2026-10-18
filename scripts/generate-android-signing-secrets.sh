#!/usr/bin/env bash

set -euo pipefail
umask 077

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
output_dir="$repo_root/android-signing"
keystore_path="$output_dir/microware-release.jks"
secrets_path="$output_dir/github-secrets.txt"

for executable in keytool openssl base64 tr; do
    if ! command -v "$executable" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$executable" >&2
        exit 1
    fi
done

if ! mkdir -m 700 "$output_dir"; then
    printf 'Output directory already exists; move or rename it first:\n%s\n' "$output_dir" >&2
    exit 1
fi

keystore_password="$(openssl rand -hex 32)"
key_password="$(openssl rand -hex 32)"
key_alias="microware-release"

export MICROWARE_ANDROID_STORE_PASSWORD="$keystore_password"
export MICROWARE_ANDROID_KEY_PASSWORD="$key_password"

keytool -genkeypair \
    -v \
    -keystore "$keystore_path" \
    -storetype JKS \
    -storepass:env MICROWARE_ANDROID_STORE_PASSWORD \
    -keypass:env MICROWARE_ANDROID_KEY_PASSWORD \
    -alias "$key_alias" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=MicroWare Release"

unset MICROWARE_ANDROID_STORE_PASSWORD MICROWARE_ANDROID_KEY_PASSWORD

keystore_base64="$(base64 < "$keystore_path" | tr -d '\r\n')"

{
    printf 'ANDROID_KEYSTORE_BASE64=%s\n' "$keystore_base64"
    printf 'ANDROID_KEYSTORE_PASSWORD=%s\n' "$keystore_password"
    printf 'ANDROID_KEY_ALIAS=%s\n' "$key_alias"
    printf 'ANDROID_KEY_PASSWORD=%s\n' "$key_password"
} > "$secrets_path"

chmod 600 "$keystore_path" "$secrets_path"

printf 'Created Android signing files with private permissions:\n'
printf '  Keystore: %s\n' "$keystore_path"
printf '  GitHub secrets: %s\n' "$secrets_path"
printf '\nCopy each NAME=VALUE line from github-secrets.txt into repository Actions secrets.\n'
printf 'Keep both files backed up securely; do not commit or share them.\n'
