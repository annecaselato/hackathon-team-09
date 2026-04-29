variable "name_prefix" {
  type = string
}

variable "location" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "tenant_id" {
  type = string
}

variable "db_admin_username" {
  type = string
}

variable "db_admin_password" {
  type      = string
  sensitive = true
}

variable "db_connection_string" {
  type      = string
  sensitive = true
}

variable "app_insights_connection" {
  type      = string
  sensitive = true
}

variable "tags" {
  type = map(string)
}
