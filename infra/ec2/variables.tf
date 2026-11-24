variable "region" {
  description = "Región de AWS."
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "Tipo de instancia. Debe ser uno que la consola marque como 'Free tier eligible' en tu cuenta (t3.micro o t2.micro suelen serlo)."
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "Nombre de un par de llaves EC2 ya creado en la consola, para entrar por SSH."
  type        = string
}

variable "ssh_cidr" {
  description = "Rango de IP que puede entrar por SSH (22). Usa tu IP pública con /32."
  type        = string
}

variable "imagen" {
  description = "Imagen de la API ya subida a un registro (por ejemplo ghcr.io/ORG/facturacion-liquidacion:1.0.0)."
  type        = string
}

variable "db_password" {
  description = "Contraseña de PostgreSQL."
  type        = string
  sensitive   = true
}

variable "swap_gb" {
  description = "Tamaño del swap en GiB (la instancia tiene poca RAM)."
  type        = number
  default     = 1
}
