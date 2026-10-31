# Account Service (Microservicio de Cuentas)

Microservicio encargado de la gestión de cuentas corrientes, administración de saldos y operaciones de débito/crédito. Forma parte del sistema de transferencias bancarias y se comunica de manera síncrona vía HTTP REST con el microservicio de clientes (`customer-service`).

---

## 1. Descripción de la Arquitectura

* **Framework:** Quarkus (Java 17+)
* **Capa Web:** RESTEasy Reactive con serialización JSON (Jackson) y Bean Validation
* **Cliente HTTP:** Quarkus REST Client Reactive (comunicación síncrona inter-servicio)
* **Persistencia:** Hibernate ORM con Panache (patrón Active Record)
* **Base de Datos:** PostgreSQL independiente (`account_db`)
* **Observabilidad:** SmallRye Health (`/q/health`) y Micrometer Prometheus (`/q/metrics`)
* **Comunicación:** Síncrona vía HTTP/REST

### Integración Inter-Servicio
Al registrar una nueva cuenta corriente, `account-service` invoca síncronamente el endpoint `GET /customers/{id}` de `customer-service` mediante `CustomerRestClient`. Si el cliente no existe o el servicio no responde, la creación de la cuenta se cancela inmediatamente garantizando consistencia referencial.

---

## 2. Instrucciones de Ejecución

### Prerrequisitos
* Java Development Kit (JDK) 17 o superior
* PostgreSQL ejecutándose localmente en el puerto `5432` con una base de datos llamada `account_db`
* Microservicio `customer-service` corriendo en `http://localhost:8081`

### Pasos para iniciar

1. Clonar el repositorio y entrar a la carpeta del proyecto:
   ```bash
   cd account-service
   ```

2. Verificar o ajustar credenciales en `src/main/resources/application.properties`:
   ```properties
   quarkus.http.port=8082
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/account_db
   quarkus.datasource.username=postgres
   quarkus.datasource.password=postgres
   quarkus.rest-client.customer-api.url=http://localhost:8081
   ```

3. Ejecutar en modo desarrollo:
   ```bash
   ./mvnw quarkus:dev
   ```
   *En Windows:*
   ```cmd
   mvnw.cmd quarkus:dev
   ```

El servicio iniciará en: `http://localhost:8082`

---

## 3. Endpoints Disponibles

| Método | Endpoint | Descripción | Requiere Auth |
| :--- | :--- | :--- | :--- |
| **GET** | `/accounts/{number}` | Consulta detalle de cuenta por número y saldo disponible (RF2) | No |
| **GET** | `/accounts/customer/{customerId}` | Lista todas las cuentas asociadas a un cliente (RF2) | No |
| **POST** | `/accounts` | Crea una cuenta validando síncronamente que el cliente exista | No |
| **PUT** | `/accounts/{number}/debit?amount=X` | Realiza débito de saldo (validando fondos suficientes) | Interno |
| **PUT** | `/accounts/{number}/credit?amount=X` | Realiza acreditación/abono de saldo | Interno |
| **GET** | `/q/health` | Health checks de Liveness y Readiness | No |
| **GET** | `/q/metrics` | Métricas del servicio en formato Prometheus | No |

---

## 4. Ejemplos de Uso (cURL)

### Crear Cuenta Corriente (Validación Exitosa)
```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "456-789-001",
    "customerId": 1,
    "balance": 5000.00,
    "accountType": "CHECKING",
    "status": "ACTIVE"
  }'
```

**Respuesta (201 Created):**
```json
{
  "id": 1,
  "accountNumber": "456-789-001",
  "customerId": 1,
  "balance": 5000.00,
  "accountType": "CHECKING",
  "status": "ACTIVE"
}
```

### Consultar Cuenta por Número
```bash
curl -X GET http://localhost:8082/accounts/456-789-001
```

**Respuesta (200 OK):**
```json
{
  "id": 1,
  "accountNumber": "456-789-001",
  "customerId": 1,
  "balance": 5000.00,
  "accountType": "CHECKING",
  "status": "ACTIVE"
}
```

### Listar Cuentas de un Cliente
```bash
curl -X GET http://localhost:8082/accounts/customer/1
```

### Debitar Saldo
```bash
curl -X PUT "http://localhost:8082/accounts/456-789-001/debit?amount=200.00"
```

### Acreditar Saldo
```bash
curl -X PUT "http://localhost:8082/accounts/456-789-001/credit?amount=300.00"
```

---

## 5. Datos de Prueba

### Conexión a Base de Datos Local
* **Host:** `localhost:5432`
* **Base de Datos:** `account_db`
* **Usuario / Contraseña:** `postgres` / `postgres`

### Cuentas Recomendadas para Pruebas de Transferencia
* **Cuenta Origen:** `456-789-001` (Saldo inicial: 5000.00, Cliente: 1)
* **Cuenta Destino:** `456-789-002` (Saldo inicial: 1000.00, Cliente: 1)