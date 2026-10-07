# Delivery pipeline

```
PR ──▶ pull-request.yml ── title lint (conventional commits)
                        └─ ci.yml: gradle tests/lint/builds · docker build · terraform fmt/validate · actionlint
merge to main ──▶ release.yml
   ci ─▶ semantic-release (tag vX.Y.Z + GitHub release, only for feat/fix/perf/breaking)
        ├─▶ image: fat jar once, then multi-arch image (arm64 + amd64) → ghcr.io/plokkke/baby-phone-server (public)
        │     └─▶ deploy.yml: deployment request (GitHub Deployments API) for vX.Y.Z
        └─▶ play (only when vars.PLAY_PUBLISH == 'true'): signed AAB → Play internal track
```

Squash-merge PRs: the PR title becomes the commit semantic-release analyses.
`feat:` → minor, `fix:`/`perf:` → patch, `feat!:` / `BREAKING CHANGE:` → major, others → no release.

## Deployment

The Raspberry Pi is run by [`Plokkke/home-platform`](https://github.com/Plokkke/home-platform) (see its `docs/architecture.md`). Its **deployer** polls this repository's deployment requests, applies `infrastructure/terraform/server` at the requested ref with `image_tag=<version>`, checks `health_url` and reports the result in the **Deployments** tab. GitHub never reaches the Pi and holds no deployment secret.

| Need | How |
|---|---|
| Deploy a release | automatic: `release.yml` calls `deploy.yml` with the new version |
| Infrastructure change only | automatic: a push to `main` touching `infrastructure/terraform/server/**` redeploys the current version with the new stack |
| Rollback | `gh workflow run deploy -f version=<previous>` (or *Run workflow* in the Actions tab) |

The stack also registers the HTTPS site `babyphone.crn-tech.fr` on the host nginx (`register-service.sh`, certificate included). The `play` stack is not deployed by the deployer: apply it from a workstation.

## Local Terraform

```bash
ssh -N -L 5432:127.0.0.1:5432 pi &          # the Pi's shared Postgres holds the state
export PG_CONN_STR=postgres://terraform:<password>@127.0.0.1:5432/tf_backend?sslmode=disable   # password: home-platform secrets
terraform -chdir=infrastructure/terraform/server init
terraform -chdir=infrastructure/terraform/server plan -var image_tag=<version> -var docker_host=ssh://<user>@pi
```
Plans only: applies go through a deployment request.
