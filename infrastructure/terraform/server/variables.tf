variable "image_repository" {
  description = "Server image repository, e.g. ghcr.io/owner/baby-phone-server"
  type        = string
}

variable "image_tag" {
  description = "Semantic version to deploy"
  type        = string
}

variable "docker_host" {
  description = "Docker API endpoint; the deploy runner sits on the Raspberry Pi itself"
  type        = string
  default     = "unix:///var/run/docker.sock"
}

variable "registry_username" {
  type = string
}

variable "registry_password" {
  type      = string
  sensitive = true
}

variable "host_port" {
  description = "Loopback port the host nginx proxies to"
  type        = number
  default     = 18080
}

variable "android_package" {
  type    = string
  default = "fr.crntech.babyphone"
}

variable "android_cert_fingerprints" {
  description = "SHA-256 of every certificate signing the app (debug, upload, Play app signing) for App Links"
  type        = list(string)
}
