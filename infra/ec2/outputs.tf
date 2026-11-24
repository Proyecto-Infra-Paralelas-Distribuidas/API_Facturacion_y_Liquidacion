output "ip_publica" {
  value = aws_instance.api.public_ip
}

output "url_api" {
  description = "URL base para registrar en la hoja Coordinacion_Voltrix."
  value       = "http://${aws_instance.api.public_ip}:8080"
}

output "url_contrato" {
  value = "http://${aws_instance.api.public_ip}:8080/openapi.yaml"
}

output "url_health" {
  value = "http://${aws_instance.api.public_ip}:8080/health"
}
