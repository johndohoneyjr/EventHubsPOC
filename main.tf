terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=2.46.0"
    }
  }
    backend "azurerm" {
        resource_group_name  = "dohoney-devops-rg"
        storage_account_name = "dohoneytf"
        container_name       = "tfstatedohoney"
        key                  = "terraform.tfstate"
    }

}

provider "azurerm" {
        features {}
}
resource "azurerm_resource_group" "messaging_group" {
  name     = "event-hub-poc-rg"
  location = "West US"
}

resource "azurerm_eventhub_namespace" "hub_namespace" {
  name                = "poc-event-hub-uswest"
  location            = azurerm_resource_group.messaging_group.location
  resource_group_name = azurerm_resource_group.messaging_group.name
  sku                 = "Standard"
  capacity            = 1

  tags = {
    environment = "Production"
  }
}

resource "azurerm_eventhub" "marketing_hub" {
  name                = "Marketing"
  namespace_name      = azurerm_eventhub_namespace.hub_namespace.name
  resource_group_name = azurerm_resource_group.messaging_group.name
  partition_count     = 2
  message_retention   = 7
}


resource "azurerm_eventhub_authorization_rule" "marketing_rule" {
  name                = "marketingRule"
  namespace_name      = azurerm_eventhub_namespace.hub_namespace.name
  eventhub_name       = azurerm_eventhub.marketing_hub.name
  resource_group_name = azurerm_resource_group.messaging_group.name
  listen              = true
  send                = true
  manage              = false
}

resource "azurerm_eventhub" "customer_management" {
  name                = "CRM"
  namespace_name      = azurerm_eventhub_namespace.hub_namespace.name
  resource_group_name = azurerm_resource_group.messaging_group.name
  partition_count     = 2
  message_retention   = 7
}


resource "azurerm_eventhub_authorization_rule" "customer_management_rule" {
  name                = "crmRule"
  namespace_name      = azurerm_eventhub_namespace.hub_namespace.name
  eventhub_name       = azurerm_eventhub.customer_management.name
  resource_group_name = azurerm_resource_group.messaging_group.name
  listen              = true
  send                = true
  manage              = false
}

