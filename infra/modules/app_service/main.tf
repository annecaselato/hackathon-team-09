resource "azurerm_service_plan" "this" {
  name                = "plan-${var.name_prefix}"
  resource_group_name = var.resource_group_name
  location            = var.location
  os_type             = "Linux"
  sku_name            = "B1"
  tags                = var.tags
}

resource "azurerm_linux_web_app" "backend" {
  name                = "app-${var.name_prefix}-backend"
  resource_group_name = var.resource_group_name
  location            = var.location
  service_plan_id     = azurerm_service_plan.this.id
  https_only          = true
  tags                = var.tags

  identity {
    type = "SystemAssigned"
  }

  site_config {
    application_stack {
      docker_image_name = var.backend_image
    }
    always_on = false
  }

  app_settings = {
    WEBSITES_PORT                          = "8080"
    SPRING_PROFILES_ACTIVE                 = "azure"
    SPRING_DATASOURCE_URL                  = var.db_connection_string
    SPRING_DATASOURCE_USERNAME             = var.db_admin_username
    SPRING_DATASOURCE_PASSWORD             = var.db_admin_password
    APPLICATIONINSIGHTS_CONNECTION_STRING  = var.app_insights_connection_string
  }
}

resource "azurerm_linux_web_app" "frontend" {
  name                = "app-${var.name_prefix}-frontend"
  resource_group_name = var.resource_group_name
  location            = var.location
  service_plan_id     = azurerm_service_plan.this.id
  https_only          = true
  tags                = var.tags

  identity {
    type = "SystemAssigned"
  }

  site_config {
    application_stack {
      docker_image_name = var.frontend_image
    }
    always_on = false
  }

  app_settings = {
    WEBSITES_PORT                         = "3000"
    NEXT_PUBLIC_API_BASE_URL              = "https://${azurerm_linux_web_app.backend.default_hostname}"
    APPLICATIONINSIGHTS_CONNECTION_STRING = var.app_insights_connection_string
  }
}
