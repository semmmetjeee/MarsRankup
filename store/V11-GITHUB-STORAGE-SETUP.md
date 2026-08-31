# Semmmetje Store v11 — GitHub private storage

This replaces Cloudflare R2 entirely.

## Required Worker bindings/secrets
- `DB` → D1 `mars-v3`
- `APP_SECRET` → existing encrypted secret
- `GITHUB_TOKEN` → fine-grained token scoped only to `semmmetjeee/Semmmetje-Store-Files`

The GitHub token needs **Repository permissions → Contents: Read and write** if the admin panel should upload banners and release files. Read-only is sufficient only for protected downloads.

## Storage repository
Private repository: `semmmetjeee/Semmmetje-Store-Files`

Files are stored under:
- `banners/<product-slug>/...`
- `releases/<product-slug>/<version>/...`

The frontend never receives the GitHub token. Protected release downloads are proxied through the Worker after login + ownership checks.

## Database
Keep the existing v9/v10 `releases` and `download_log` tables. The `releases.object_key` field stores the path inside the private GitHub repository.
