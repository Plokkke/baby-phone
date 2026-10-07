variable "image_repository" {
  description = "Server image repository"
  type        = string
  default     = "ghcr.io/plokkke/baby-phone-server"
}

variable "image_tag" {
  description = "Semantic version to deploy"
  type        = string

  validation {
    condition     = var.image_tag != ""
    error_message = "image_tag is required: deploy a released version."
  }
}

variable "docker_host" {
  description = "Docker API endpoint; the deployer runs on the Raspberry Pi itself"
  type        = string
  default     = "unix:///var/run/docker.sock"
}

variable "subdomain" {
  description = "https://<subdomain>.crn-tech.fr"
  type        = string
  default     = "babyphone"
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

variable "play_store_url" {
  description = "Play Store listing Android devices without the app are sent to; empty until published"
  type        = string
  default     = ""
}
