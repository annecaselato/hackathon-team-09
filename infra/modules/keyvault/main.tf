data "azurerm_client_config" "current" {}

resource "random_string" "suffix" {
  length  = 5
  upper   = false
  special = false
}

resource "azurerm_key_vault" "this" {
  name                        = "kv-${replace(var.name_prefix, "-", "")}-${random_string.suffix.result}"
  location                    = var.location
  resource_group_name         = var.resource_group_name
  tenant_id                   = var.tenant_id
  sku_name                    = "standard"
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false
  enabled_for_disk_encryption = true
  tags                        = var.tags

  access_policy {
    tenant_id = var.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = ["Get", "Set", "List", "Delete", "Recover", "Purge"]
  }
}

resource "azurerm_key_vault_secret" "db_username" {
  name         = "db-username"
  value        = var.db_admin_username
  key_vault_id = azurerm_key_vault.this.id
}

resource "azurerm_key_vault_secret" "db_password" {
  name         = "db-password"
  value        = var.db_admin_password
  key_vault_id = azurerm_key_vault.this.id
}

resource "azurerm_key_vault_secret" "db_connection" {
  name         = "db-connection-string"
  value        = var.db_connection_string
  key_vault_id = azurerm_key_vault.this.id
}

resource "azurerm_key_vault_secret" "app_insights_connection" {
  name         = "appinsights-connection-string"
  value        = var.app_insights_connection
  key_vault_id = azurerm_key_vault.this.id
}
