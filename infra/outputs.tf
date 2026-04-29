output "resource_group_name" {
  description = "Azure resource group name."
  value       = module.resource_group.resource_group_name
}

output "backend_url" {
  description = "Public backend endpoint."
  value       = module.app_service.backend_url
}

output "frontend_url" {
  description = "Public frontend endpoint."
  value       = module.app_service.frontend_url
}

output "key_vault_name" {
  description = "Azure Key Vault name."
  value       = module.keyvault.key_vault_name
}

output "postgres_fqdn" {
  description = "PostgreSQL server FQDN."
  value       = module.database.postgres_fqdn
}
