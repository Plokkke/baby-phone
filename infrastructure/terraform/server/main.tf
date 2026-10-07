locals {
  image = "${var.image_repository}:${var.image_tag}"
}

data "docker_registry_image" "server" {
  name = local.image
}

resource "docker_image" "server" {
  name          = local.image
  pull_triggers = [data.docker_registry_image.server.sha256_digest]
  keep_locally  = true
}

resource "docker_container" "server" {
  name    = "babyphone-server"
  image   = docker_image.server.image_id
  restart = "unless-stopped"

  env = [
    "APP_VERSION=${var.image_tag}",
    "ANDROID_PACKAGE=${var.android_package}",
    "ANDROID_CERT_SHA256=${join(",", var.android_cert_fingerprints)}",
    "PLAY_STORE_URL=${var.play_store_url}",
  ]

  ports {
    internal = 8080
    external = var.host_port
    ip       = "127.0.0.1"
  }

  log_driver = "json-file"
  log_opts = {
    max-size = "10m"
    max-file = "3"
  }
}

# HTTPS site on the host nginx (certificate included); kept as is when it already exists.
resource "terraform_data" "site" {
  input = { subdomain = var.subdomain, port = var.host_port }

  provisioner "local-exec" {
    command = "[ -e /etc/nginx/sites-available/${self.input.subdomain}.crn-tech.fr ] || sudo /etc/nginx/register-service.sh ${self.input.subdomain} ${self.input.port}"
  }

  provisioner "local-exec" {
    when    = destroy
    command = "sudo /etc/nginx/unregister-service.sh ${self.input.subdomain}"
  }

  depends_on = [docker_container.server]
}
