variable "name_prefix" {
  type = string
}

variable "location" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "backend_image" {
  type = string
}

variable "frontend_image" {
  type = string
}

variable "app_insights_connection_string" {
  type      = string
  sensitive = true
}

variable "db_connection_string" {
  type      = string
  sensitive = true
}

variable "db_admin_username" {
  type = string
}

variable "db_admin_password" {
  type      = string
  sensitive = true
}

variable "tags" {
  type = map(string)
}
