# Infrastructure side of Google Play publishing. The upload itself is done in CI
# by Gradle Play Publisher. Terraform cannot do the Play Console steps:
# creating the app, uploading the first AAB, inviting the service account.

resource "google_project_service" "android_publisher" {
  service            = "androidpublisher.googleapis.com"
  disable_on_destroy = false
}

resource "google_service_account" "play_publisher" {
  account_id   = "play-publisher"
  display_name = "Google Play publisher (GitHub Actions)"
}

resource "google_service_account_key" "play_publisher" {
  service_account_id = google_service_account.play_publisher.name
}

resource "github_actions_secret" "play_credentials" {
  repository      = var.github_repository
  secret_name     = "PLAY_SERVICE_ACCOUNT_JSON"
  plaintext_value = base64decode(google_service_account_key.play_publisher.private_key)
}

resource "github_actions_variable" "play_publish" {
  repository    = var.github_repository
  variable_name = "PLAY_PUBLISH"
  value         = tostring(var.enable_publishing)
}
