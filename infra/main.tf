locals {
  common_tags = {
    project     = var.project_name
    environment = var.environment
    owner       = var.owner
    managed_by  = "terraform"
  }

  name_suffix = "${var.project_name}-${var.environment}"
}

module "resource_group" {
  source = "./modules/resource_group"

  resource_group_name = "rg-${local.name_suffix}"
  location            = var.location
  tags                = local.common_tags
}

module "monitoring" {
  source = "./modules/monitoring"

  name_prefix         = local.name_suffix
  location            = var.location
  resource_group_name = module.resource_group.resource_group_name
  tags                = local.common_tags
}

module "database" {
  source = "./modules/database"

  name_prefix         = local.name_suffix
  location            = var.location
  resource_group_name = module.resource_group.resource_group_name
  db_admin_username   = var.db_admin_username
  db_admin_password   = var.db_admin_password
  tags                = local.common_tags
}

module "keyvault" {
  source = "./modules/keyvault"

  name_prefix              = local.name_suffix
  location                 = var.location
  resource_group_name      = module.resource_group.resource_group_name
  tenant_id                = module.database.tenant_id
  db_admin_username        = var.db_admin_username
  db_admin_password        = var.db_admin_password
  db_connection_string     = module.database.jdbc_connection_string
  app_insights_connection  = module.monitoring.application_insights_connection_string
  tags                     = local.common_tags
}

module "app_service" {
  source = "./modules/app_service"

  name_prefix                        = local.name_suffix
  location                           = var.location
  resource_group_name                = module.resource_group.resource_group_name
  backend_image                      = var.backend_image
  frontend_image                     = var.frontend_image
  app_insights_connection_string     = module.monitoring.application_insights_connection_string
  db_connection_string               = module.database.jdbc_connection_string
  db_admin_username                  = var.db_admin_username
  db_admin_password                  = var.db_admin_password
  tags                               = local.common_tags
}
