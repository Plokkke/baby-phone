# Delivery pipeline

```
PR ──▶ pull-request.yml ── title lint (conventional commits)
                        └─ ci.yml: gradle tests/lint/builds · docker build · terraform fmt/validate · actionlint
merge to main ──▶ release.yml
   ci ─▶ semantic-release (tag vX.Y.Z + GitHub release, only for feat/fix/perf/breaking)
        ├─▶ image: fat jar once, then multi-arch image (arm64 + amd64) → ghcr.io/plokkke/baby-phone-server
        │     └─▶ deploy (self-hosted runner on the RPi): terraform apply → smoke test /health version
        └─▶ play (only when vars.PLAY_PUBLISH == 'true'): signed AAB → Play internal track
```

Squash-merge PRs: the PR title becomes the commit semantic-release analyses.
`feat:` → minor, `fix:`/`perf:` → patch, `feat!:` / `BREAKING CHANGE:` → major, others → no release.

## One-time Raspberry Pi setup

1. **Terraform state backend** in `/srv/docker/tf-backend` (Postgres, loopback only, shared by all Terraform projects):
   ```bash
   mkdir -p /srv/docker/tf-backend && cd /srv/docker/tf-backend
   curl -fsSLO https://raw.githubusercontent.com/Plokkke/baby-phone/main/infrastructure/bootstrap/docker-compose.yml
   openssl rand -hex 24 > tf-state-password && chmod 600 tf-state-password
   docker compose up -d
   ```
   Back it up with the rest of `/srv/docker`: losing it means Terraform forgets what it manages.
2. **GitHub runner** (Settings → Actions → Runners → New self-hosted runner → Linux ARM64), then:
   ```bash
   ./config.sh --url https://github.com/Plokkke/baby-phone --token <token> --labels rpi --unattended
   sudo ./svc.sh install && sudo ./svc.sh start
   sudo usermod -aG docker <runner-user>   # Terraform talks to the local Docker socket
   ```
   Needs `curl` and `unzip` (terraform download).
3. **Secret** `TF_STATE_PG_CONN_STR` in the `production` environment:
   `postgres://terraform:<password>@127.0.0.1:5433/terraform?sslmode=disable`
4. **nginx**: install `infrastructure/nginx/babyphone.conf` (adapt certificate paths), `nginx -t && systemctl reload nginx`.

## Public repository + self-hosted runner

A self-hosted runner on a public repo can run code from strangers. Safeguards in place:
- PR workflows only use GitHub-hosted runners; `rpi` is referenced by `release.yml` on `main` only.
- Fork PR workflows require approval for every external contributor.
- The `production` environment only accepts deployments from `main`.
- Branch ruleset on `main`: PR required, CI must pass, no force push.
Still: never approve a fork PR that touches `.github/` without reading it.

## Local Terraform

```bash
ssh -N -L 5433:127.0.0.1:5433 rpi &          # reach the state database
export PG_CONN_STR=postgres://terraform:<password>@127.0.0.1:5433/terraform?sslmode=disable
terraform -chdir=infrastructure/terraform/server init
terraform -chdir=infrastructure/terraform/server plan \
  -var image_repository=ghcr.io/plokkke/baby-phone-server -var image_tag=<version> \
  -var registry_username=<github user> -var registry_password=$(gh auth token)
```
Plan from the laptop needs `-var docker_host=ssh://<user>@rpi`.
