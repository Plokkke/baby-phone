# Google Play publishing (prepared, disabled)

There is no Terraform provider for Play releases: Terraform owns the *infrastructure*
(API, service account, CI secret) and Gradle Play Publisher uploads the *artifact*.

## Steps once the developer account exists
1. Create the app `fr.crntech.babyphone` in Play Console, enable Play App Signing.
2. Create an upload key and store it as secrets in the `google-play` environment:
   ```bash
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
   gh secret set ANDROID_KEYSTORE_BASE64 --env google-play < <(base64 -i upload.jks)
   gh secret set ANDROID_KEYSTORE_PASSWORD --env google-play
   gh secret set ANDROID_KEY_ALIAS --env google-play --body upload
   gh secret set ANDROID_KEY_PASSWORD --env google-play
   ```
3. Upload the **first** AAB by hand (Play requirement):
   `ANDROID_KEYSTORE_PATH=upload.jks ... ./gradlew :android:bundleRelease -Pbabyphone.version=x.y.z`
4. `terraform -chdir=infrastructure/terraform/play apply -var gcp_project_id=<project>`
   → enables the API, creates the service account, stores `PLAY_SERVICE_ACCOUNT_JSON` in GitHub.
5. Play Console → Users and permissions → invite the output `service_account_email` (release rights).
6. `terraform apply -var enable_publishing=true` → sets `PLAY_PUBLISH=true`; every release now reaches the internal track.
7. Set `play_store_url` in `infrastructure/terraform/server/terraform.tfvars`: phones without the app
   scanning a pairing QR are then sent to the listing.
8. Add the **upload** and **Play App Signing** SHA-256 to `infrastructure/terraform/server/terraform.tfvars`,
   otherwise App Links (system camera → app) break for store installs.

Note: the service account key lives in the Terraform state (Postgres on the RPi) and in GitHub secrets.
