# Account Service (Microservicio de Cuentas)

Microservicio encargado de la gestión de cuentas corrientes, administración de saldos y operaciones de débito/crédito. Forma parte del sistema de transferencias bancarias y se comunica de manera síncrona vía HTTP REST con el microservicio de clientes (`customer-service`).

---

## 1. Descripción de la Arquitectura

- **Framework:** Quarkus (Java 21+)
- **Capa Web:** RESTEasy Reactive con serialización JSON (Jackson) y Bean Validation
- **Cliente HTTP:** Quarkus REST Client Reactive (comunicación síncrona inter-servicio)
- **Persistencia:** Hibernate ORM con Panache (patrón Active Record)
- **Base de Datos:** PostgreSQL independiente (`account_db`)
- **Seguridad:** Autenticación y Autorización basada en JWT (SmallRye JWT) validando firmas asimétricas RSA delegadas.
- **Observabilidad:** SmallRye Health (Liveness/Readiness), Micrometer Prometheus (`/q/metrics`) y Logs estructurados en JSON.
- **Comunicación:** Síncrona vía HTTP/REST

### Integración Inter-Servicio
Al registrar una nueva cuenta corriente, `account-service` invoca síncronamente el endpoint `GET /customers/{id}` de `customer-service` mediante `CustomerRestClient`. Si el cliente no existe o el servicio no responde, la creación de la cuenta se cancela inmediatamente garantizando consistencia referencial. Además, los tokens de seguridad y los Correlation IDs se propagan automáticamente en estas peticiones internas.

---

## 2. Instrucciones de Ejecución

### Prerrequisitos
- Java Development Kit (JDK) 17 o superior
- PostgreSQL ejecutándose localmente en el puerto `5432` con una base de datos llamada `account_db`
- Microservicio `customer-service` corriendo en `http://localhost:8081`

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
| **GET** | `/accounts/{number}` | Consulta detalle de cuenta por número (RF2) | No (Validación interna) |
| **GET** | `/accounts/customer/{customerId}` | Lista todas las cuentas asociadas a un cliente (RF2) | No (Validación interna) |
| **POST** | `/accounts` | Crea cuenta validando contra el servicio de clientes | **Sí (ROLE_ADMIN / ROLE_USER)** |
| **PUT** | `/accounts/{number}/debit?amount=X` | Débito de saldo (validando fondos) | **Sí (ROLE_ADMIN / ROLE_USER)** |
| **PUT** | `/accounts/{number}/credit?amount=X` | Acreditación/abono de saldo | **Sí (ROLE_ADMIN / ROLE_USER)** |
| **GET** | `/q/health` | Health checks de Liveness y Readiness | No |
| **GET** | `/q/metrics` | Métricas del servicio en formato Prometheus | No |

---

## 4. Observabilidad y Monitoreo

Este microservicio cumple con los estándares de observabilidad cloud-native:

- **Health Checks (`/q/health`):** Incluye validación de conexión a PostgreSQL y un validador personalizado (`AccountReadinessCheck`) para confirmar la inicialización de los clientes REST.
- **Métricas (`/q/metrics`):** Expone métricas de la JVM, uso de memoria, CPU y tiempos de respuesta HTTP en formato estándar de Prometheus.
- **Logs Estructurados:** Emite logs en formato JSON (`quarkus.log.console.json=true`), inyectando automáticamente el `correlationId` para facilitar la trazabilidad distribuida.

---

## 5. Seguridad (JWT)

El servicio protege sus endpoints de modificación de estado (creación, débito, crédito) utilizando **SmallRye JWT**. Valida los tokens utilizando la llave pública compartida (`mp.jwt.verify.publickey`) sin necesidad de llamar a un servidor de identidades externo en cada petición.

> **🔒 Nota de Seguridad sobre JWT y Credenciales**
> Las llaves criptográficas para la verificación de tokens JWT y las credenciales de base de datos incluidas en este repositorio se proveen "hardcodeadas" de manera intencional en el archivo `application.properties`, exclusivamente para facilitar la ejecución, evaluación y pruebas de este reto técnico en un entorno local.
>
> En un entorno de producción real, estos valores jamás se versionan en Git. La arquitectura correcta implica inyectar estos valores en tiempo de despliegue mediante variables de entorno o utilizar un gestor centralizado de secretos (como AWS Secrets Manager o HashiCorp Vault).

---

## 6. Ejemplos de Uso (cURL)

*Nota: Reemplaza `<TOKEN_AQUI>` con un JWT válido generado en la interfaz de desarrollo.*

### Crear Cuenta Corriente (Protegido)
```bash
curl -X POST http://localhost:8082/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_AQUI>" \
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

### Consultar Cuenta por Número (Público/Interno)
```bash
curl -X GET http://localhost:8082/accounts/456-789-001
```

### Debitar Saldo (Protegido)
```bash
curl -X PUT "http://localhost:8082/accounts/456-789-001/debit?amount=200.00" \
  -H "Authorization: Bearer <TOKEN_AQUI>"
```

### Acreditar Saldo (Protegido)
```bash
curl -X PUT "http://localhost:8082/accounts/456-789-001/credit?amount=300.00" \
  -H "Authorization: Bearer <TOKEN_AQUI>"
```

---

## 7. Datos de Prueba

### Conexión a Base de Datos Local
- **Host:** `localhost:5432`
- **Base de Datos:** `account_db`
- **Usuario / Contraseña:** `postgres` / `postgres`

### Cuentas Recomendadas para Pruebas de Transferencia
- **Cuenta Origen:** `456-789-001` (Saldo inicial: 5000.00, Cliente: 1)
- **Cuenta Destino:** `456-789-002` (Saldo inicial: 1000.00, Cliente: 1)