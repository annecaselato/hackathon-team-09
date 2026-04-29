variable "project_name" {
  description = "Project identifier used in resource names."
  type        = string
  default     = "sifap"
}

variable "environment" {
  description = "Deployment environment (dev, stage, prod)."
  type        = string
}

variable "location" {
  description = "Azure region for resources."
  type        = string
}

variable "owner" {
  description = "Resource owner tag value."
  type        = string
}

variable "backend_image" {
  description = "Backend container image (GHCR)."
  type        = string
}

variable "frontend_image" {
  description = "Frontend container image (GHCR)."
  type        = string
}

variable "db_admin_username" {
  description = "PostgreSQL admin user."
  type        = string
}

variable "db_admin_password" {
  description = "PostgreSQL admin password."
  type        = string
  sensitive   = true
}
