terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# Amazon Linux 2023 más reciente, tomada del parámetro público de AWS
data "aws_ssm_parameter" "ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# Red por defecto de la cuenta (suficiente para el proyecto)
data "aws_vpc" "default" {
  default = true
}

resource "aws_security_group" "api" {
  name_prefix = "equipo7-liquidacion-"
  description = "Equipo 7: API 8080 abierta y SSH solo desde ssh_cidr"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "API"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "equipo7-liquidacion", Equipo = "7" }
}

resource "aws_instance" "api" {
  ami                         = data.aws_ssm_parameter.ami.value
  instance_type               = var.instance_type
  key_name                    = var.key_name
  vpc_security_group_ids      = [aws_security_group.api.id]
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    imagen      = var.imagen
    db_password = var.db_password
    swap_gb     = var.swap_gb
  })
  user_data_replace_on_change = true

  root_block_device {
    volume_size = 12
    volume_type = "gp3"
  }

  tags = { Name = "equipo7-liquidacion", Equipo = "7" }
}
