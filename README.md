# Facturación y Liquidación — Equipo 7 (Avance 1)

Esqueleto de la API (Java 21, Spring Boot 3, PostgreSQL) que cumple el contrato `openapi.yaml`.

**Estado del Avance 1:** responde con datos de ejemplo guardados en la base de datos (periodo `2026-11`, plantas `PL-001` y `PL-002`).
Todavía NO consulta a Reportes de Generación (Eq 4) ni a Mantenimiento Predictivo (Eq 6) y NO calcula; eso es el Avance 2.

> **Aviso honesto:** este código se escribió sin poder compilarlo (el entorno donde se generó no tenía acceso a Maven Central). Hay que ejecutar `mvn package` una vez y corregir lo que salga. Los archivos de Terraform y la parte de Floci tampoco se pudieron ejecutar; hay que probarlos.

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/liquidaciones/generar?modo=secuencial\|paralelo&workers=N` | Devuelve las liquidaciones de ejemplo del periodo (stub) |
| GET | `/liquidaciones/{periodo}?plantaId&limit&offset` | Página de liquidaciones + total consolidado del periodo |
| GET | `/health` | `disponible` si la base de datos responde; 503 si no |
| GET | `/openapi.yaml` | El contrato |

Errores con el esquema común (`codigo`, `mensaje`, `requestId`, `detalles`); 429 con `Retry-After` cuando hay más de `MAX_CONCURRENT_JOBS` generaciones a la vez; 503 si cae la base de datos. Cada respuesta lleva `X-Request-Id` y cada petición deja una línea JSON en el log con `X-Equipo-Consumidor`.

## 1. Probar en local

```bash
cp .env.example .env          # cambia DB_PASSWORD
docker compose up --build -d
curl http://localhost:8080/health
curl -H "X-Equipo-Consumidor: equipo-7" http://localhost:8080/liquidaciones/2026-11
curl -X POST -H "Content-Type: application/json" -H "X-Equipo-Consumidor: equipo-7" \
     -d '{"periodo":"2026-11"}' "http://localhost:8080/liquidaciones/generar?modo=paralelo&workers=2"
```

Pruebas de error: periodo `2026-13` → 400; periodo `2030-01` → 404; apagar la base (`docker compose stop db`) y llamar → 503.

## 2. Subir la imagen a un registro

EC2 y el clúster descargan la imagen de un registro (no la construyen). Ejemplo con GitHub Container Registry:

```bash
docker build -t ghcr.io/<ORG>/facturacion-liquidacion:1.0.0 .
echo <TOKEN> | docker login ghcr.io -u <USUARIO> --password-stdin
docker push ghcr.io/<ORG>/facturacion-liquidacion:1.0.0
```

Hay que hacer pública la imagen (o configurar credenciales de descarga). Pon esa misma imagen en `infra/ec2/terraform.tfvars` y en `k8s/api.yaml`.

## 3. EC2 (Componente B)

Requisitos: Terraform ≥ 1.5, AWS CLI con credenciales, un par de llaves EC2 creado en la consola.

```bash
cd infra/ec2
cp terraform.tfvars.example terraform.tfvars   # completa los valores
terraform init
terraform plan
terraform apply
```

Al terminar, `terraform output url_api` da la URL que se registra en la hoja Coordinacion_Voltrix. Comprobar:

```bash
curl http://<IP>:8080/health
curl http://<IP>:8080/openapi.yaml
```

Qué crea: un Security Group (solo 8080 abierto; SSH únicamente desde `ssh_cidr`) y una instancia con Amazon Linux 2023 que instala Docker, crea 1 GiB de swap y levanta `docker-compose` con la API (`mem_limit` 400m) y PostgreSQL (`mem_limit` 256m), ambos con `restart: unless-stopped`. La base de datos no expone puertos. Si algo falla, revisar `/var/log/user-data.log` en la instancia.

El tipo de instancia por defecto es `t3.micro`; confirma en la consola de EC2 que tu cuenta lo marca como "Free tier eligible". Si no, cambia `instance_type`.

## 4. Clúster en Floci EKS

Requisitos: Docker corriendo con permisos de root y acceso al socket, Terraform, AWS CLI, kubectl.

```bash
# 1) Levantar Floci (fija la versión de la imagen cuando sepas cuál funciona)
docker run -d --name floci -p 4566:4566 -v /var/run/docker.sock:/var/run/docker.sock floci/floci:latest

# 2) Prueba de humo
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=us-east-1
aws --endpoint-url http://localhost:4566 s3 mb s3://prueba && echo "Floci responde"

# 3) Crear el clúster como código
cd infra/floci-eks
terraform init
terraform apply
```

Si Terraform da problemas, el mismo resultado se obtiene con la CLI:

```bash
aws --endpoint-url http://localhost:4566 iam create-role --role-name cluster \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"eks.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
aws --endpoint-url http://localhost:4566 eks create-cluster --name equipo7-liquidacion \
  --role-arn arn:aws:iam::000000000000:role/cluster --resources-vpc-config '{}'
aws --endpoint-url http://localhost:4566 eks wait cluster-active --name equipo7-liquidacion
```

### Despliegue en el clúster

Configura `kubectl` con el kubeconfig del clúster según la documentación de Floci (para `kubectl` necesita una llave IAM real; con `test/test` Floci la rechaza). Luego:

```bash
kubectl apply -f k8s/configmap.yaml -f k8s/secret.yaml -f k8s/db.yaml -f k8s/api.yaml
kubectl get pods            # la API debe mostrar 2 réplicas Running
kubectl get svc
kubectl logs deploy/liquidacion-api
kubectl port-forward svc/liquidacion-api 8081:8080
curl http://localhost:8081/health
```

Para la demostración: `kubectl scale deploy/liquidacion-api --replicas=3` y `kubectl delete pod <uno>` (Kubernetes lo recrea).

## Variables de configuración

| Variable | Para qué | Por defecto |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Base de datos | local / `liquidacion` |
| `TARIFA_POR_KWH` | Tarifa en COP por kWh | 450 |
| `MAX_CONCURRENT_JOBS` | Generaciones simultáneas antes de responder 429 | 2 |
| `URL_REPORTES`, `URL_MANTENIMIENTO` | Servicios de Eq 4 y Eq 6 (se usan en el Avance 2) | vacío |

## Pendientes del Avance 2

- Llamar a Reportes (`GET /reportes/generacion`, `granularidad=mes`) y a Mantenimiento (`GET /mantenimiento/ordenes`, `estado=completada`).
- Una tarea por planta con pool de hilos fijo, modo secuencial/paralelo y benchmark.
- Manejo de 502 `DEPENDENCIA_FALLIDA` cuando falle un servicio del que dependemos.
- Pruebas automáticas.
