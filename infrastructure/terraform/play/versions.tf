terraform {
  required_version = ">= 1.10"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.4"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.13"
    }
  }

  backend "pg" {
    schema_name = "babyphone_play"
  }
}

# Credentials: `gcloud auth application-default login`
provider "google" {
  project = var.gcp_project_id
}

# Credentials: GITHUB_TOKEN env var with repo admin rights (e.g. `export GITHUB_TOKEN=$(gh auth token)`)
provider "github" {
  owner = var.github_owner
}
