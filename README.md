# Encrypted File Manager

A small full-stack app that encrypts every file **before** it is stored. Upload a file in the browser, and the API encrypts it with AES-256-GCM and writes only ciphertext to object storage (MinIO / S3). Downloads are decrypted on the fly.

> **Status:** working MVP / portfolio project. It is a single-user demo with **no login or access control**, so don't expose it to the public internet or store real sensitive data in it.

## Features
- Upload, list, rename, download and delete files
- AES-256-GCM (`AES/GCM/NoPadding`, 128-bit tag, random 12-byte nonce per file); the nonce is stored with the file's metadata
- Ciphertext lives in S3-compatible storage (MinIO via Docker) or on local disk as a fallback; metadata lives in Postgres
- Master key supplied through the `APP_MASTER_KEY` environment variable (hex or base64, 256-bit)

## Stack
| Layer | Tech |
|---|---|
| API | Java 17, Spring Boot 3.3, Spring Data JPA |
| Storage | PostgreSQL 16 (metadata), MinIO / S3 (encrypted blobs) |
| Web | React 18, Vite, TypeScript, Tailwind CSS |
| Infra | Docker Compose |

## Architecture
```
Browser (React) --multipart--> Spring Boot API --AES-256-GCM encrypt--> MinIO (ciphertext)
                                      |
                                      +--> PostgreSQL (name, size, type, nonce, object key)
```

## Run it locally
Prerequisites: Docker, Node 18+.

```bash
# 1. Postgres + MinIO + API
docker compose up --build        # API on http://localhost:8080, MinIO console on :9001

# 2. Web UI
cd apps/web
npm install
npm run dev                      # http://localhost:5173 (proxies /api to the API)
```

The MinIO bucket `efm-blobs` must exist; create it from the MinIO console (http://localhost:9001) if the API reports a missing bucket.

### API
| Method | Path | Description |
|---|---|---|
| GET | `/health` | Liveness check |
| GET | `/files` | List files |
| POST | `/files` | Upload (multipart field `file`) |
| GET | `/files/{id}` | Download (decrypted) |
| PATCH | `/files/{id}` | Rename `{ "name": "..." }` |
| DELETE | `/files/{id}` | Delete |

## Security notes
- The values in `docker-compose.yml` (database/MinIO passwords and the default master key) are **demo-only**. Set your own with `APP_MASTER_KEY=$(openssl rand -hex 32) docker compose up`.
- Not yet implemented: user authentication, per-user keys / key rotation, and an envelope-encryption scheme (a KMS-wrapped data key per file).
