variable "gcp_project_id" {
  description = "Google Cloud project linked to the Play Console account"
  type        = string
}

variable "github_owner" {
  type    = string
  default = "Plokkke"
}

variable "github_repository" {
  type    = string
  default = "baby-phone"
}

variable "enable_publishing" {
  description = "Turns on the Play upload job; only after the first AAB was uploaded manually"
  type        = bool
  default     = false
}
