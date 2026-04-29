data "azurerm_client_config" "current" {}

output "postgres_fqdn" {
  value = azurerm_postgresql_flexible_server.this.fqdn
}

output "jdbc_connection_string" {
  sensitive = true
  value     = "jdbc:postgresql://${azurerm_postgresql_flexible_server.this.fqdn}:5432/${azurerm_postgresql_flexible_server_database.this.name}"
}

output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}
