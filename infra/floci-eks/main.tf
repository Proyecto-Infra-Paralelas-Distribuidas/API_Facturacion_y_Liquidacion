# Clúster EKS en Floci (emulador local de AWS). No necesita cuenta de AWS.
# Antes: Floci corriendo en el puerto 4566 con el socket de Docker (ver README).
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    iam = var.floci_endpoint
    sts = var.floci_endpoint
    eks = var.floci_endpoint
  }
}

# Rol que exige create-cluster (según la documentación de Floci, el rol debe existir antes)
resource "aws_iam_role" "cluster" {
  name = "${var.cluster_name}-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# El clúster se crea con el mismo comando que muestra la documentación de Floci.
# Se usa la CLI de AWS porque no está verificado que aws_eks_cluster acepte
# el vpc_config vacío que usa Floci.
resource "null_resource" "cluster" {
  triggers = {
    nombre   = var.cluster_name
    endpoint = var.floci_endpoint
    rol_arn  = aws_iam_role.cluster.arn
  }

  provisioner "local-exec" {
    environment = {
      AWS_ACCESS_KEY_ID     = "test"
      AWS_SECRET_ACCESS_KEY = "test"
      AWS_DEFAULT_REGION    = "us-east-1"
    }
    command = "aws --endpoint-url ${var.floci_endpoint} eks create-cluster --name ${var.cluster_name} --role-arn ${aws_iam_role.cluster.arn} --resources-vpc-config file://vpc-config.json && aws --endpoint-url ${var.floci_endpoint} eks wait cluster-active --name ${var.cluster_name}"
  }

  provisioner "local-exec" {
    when       = destroy
    on_failure = continue
    command    = "aws --endpoint-url ${self.triggers.endpoint} eks delete-cluster --name ${self.triggers.nombre}"
    environment = {
      AWS_ACCESS_KEY_ID     = "test"
      AWS_SECRET_ACCESS_KEY = "test"
      AWS_DEFAULT_REGION    = "us-east-1"
    }
  }
}

output "cluster" {
  value = var.cluster_name
}