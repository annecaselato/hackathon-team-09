# Infraestrutura Stage 4

Este diretório contém a base de IaC do Stage 4 em Terraform para Azure.

## Escopo

- Resource Group
- Monitoring (Log Analytics + Application Insights)
- PostgreSQL Flexible Server
- Key Vault com secrets de aplicação
- App Service Linux para backend e frontend em container

## Estrutura

- `main.tf`: composição dos módulos
- `variables.tf`: entradas globais
- `outputs.tf`: saídas principais
- `environments/*.tfvars`: parâmetros por ambiente
- `modules/*`: módulos por capacidade

## Como executar

1. Inicializar:

   terraform init

2. Validar:

   terraform fmt -recursive
   terraform validate

3. Planejar (exemplo dev):

   terraform plan -var-file=environments/dev.tfvars -out=tfplan-dev

4. Aplicar:

   terraform apply tfplan-dev

## Observações

- Ajuste as imagens em `environments/*.tfvars` para o seu GHCR.
- Nunca comite credenciais reais em `*.tfvars`.
- Em produção, substitua valores de senha por variáveis de pipeline/secret store.
