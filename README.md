# PagoFlex Backend

Microservicios Spring Boot de PagoFlex (DSY1105, Duoc UC). La app Android los consume por Retrofit a través del API Gateway.

Integrantes: _completar con nombres y secciones_

## Arquitectura

```
App Android (Retrofit)
        |
        v
API Gateway :8080  --- Eureka Server :8761
        |                    ^
        +-- auth-service :8081 ------------ pagoflex_auth
        +-- pagos-service :8082 ---Feign--> auth-service ------ pagoflex_pagos
        +-- recomendaciones-service :8083 -Feign--> auth-service -- pagoflex_recomendaciones
```

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| eureka-server | 8761 | - | Registro y descubrimiento de servicios |
| api-gateway | 8080 | - | Entrada única: `/auth/**`, `/pagos/**`, `/recomendaciones/**` |
| auth-service | 8081 | pagoflex_auth | Usuarios, credenciales (PBKDF2), registro, login, recuperación, perfil, avisos, comunas |
| pagos-service | 8082 | pagoflex_pagos | Compromisos, pagos y comprobantes, anulaciones, reportes de problema, logros, catálogos, empresas |
| recomendaciones-service | 8083 | pagoflex_recomendaciones | Recomendaciones de organizaciones, contacto autorizado, historial de estados, rubros |

Cada servicio tiene su propia base de datos y no comparte tablas. Los datos de otros servicios se obtienen por Feign (`pagos` y `recomendaciones` consultan a `auth` para validar roles, personas y comunas).

## Requisitos

- JDK 17 o superior
- Maven 3.9+ (o el Maven integrado de VS Code con Extension Pack for Java)
- MySQL 8 (Laragon o XAMPP) en `localhost:3306`, usuario `root`, sin clave

Las bases se crean solas (`createDatabaseIfNotExist=true`) y se cargan con los datos de ejemplo desde `data.sql` (`INSERT IGNORE`, se puede reiniciar sin duplicar). Para otra conexión usa las variables `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`.

Lombok: en VS Code instala "Lombok Annotations Support for VS Code" si el editor marca errores en getters/setters (Maven compila igual).

## Cómo levantarlo (en este orden)

Desde la carpeta `pagoflex-backend`:

```
mvn clean install -DskipTests
mvn -pl eureka-server spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl pagos-service spring-boot:run
mvn -pl recomendaciones-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Cada comando en una terminal distinta. Eureka queda en http://localhost:8761 y deben aparecer los 4 servicios registrados.

## Swagger / OpenAPI

| Servicio | URL |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| pagos-service | http://localhost:8082/swagger-ui.html |
| recomendaciones-service | http://localhost:8083/swagger-ui.html |

El JSON OpenAPI de cada uno está en `/v3/api-docs`.

## Endpoints principales (siempre a través del Gateway, puerto 8080)

auth-service
- `POST /auth/login`, `POST /auth/registro`, `POST /auth/recuperacion`, `POST /auth/restablecer`
- `PUT /auth/usuarios/{id}/clave`, `GET /auth/usuarios/{id}`, `GET /auth/usuarios/rut/{rut}`, `GET /auth/usuarios?ids=1,2`
- `PUT /auth/usuarios/{id}`, `PUT /auth/usuarios/{id}/avisos`, `POST /auth/usuarios/personas`, `GET /auth/comunas`

pagos-service
- `GET /pagos/catalogos`
- `GET /pagos/compromisos?idUsuario|idEmpresa|ids`, `GET /pagos/compromisos/{id}`, `POST /pagos/compromisos`
- `POST /pagos/compromisos/{id}/pago`, `PUT /pagos/compromisos/{id}/anulacion`
- `GET /pagos/pagos`, `GET /pagos/anulaciones`, `POST /pagos/vencimientos`
- `GET|POST /pagos/reportes`, `GET /pagos/reportes/{id}`, `PUT /pagos/reportes/{id}/toma`, `PUT /pagos/reportes/{id}/resolucion`
- `GET|POST /pagos/reconocimientos`

recomendaciones-service
- `GET /recomendaciones/catalogos`
- `GET /recomendaciones?idUsuario=`, `GET /recomendaciones/{id}`, `GET /recomendaciones/resumen?idUsuario=`
- `POST /recomendaciones`, `PUT /recomendaciones/{id}/estado`

Los errores de negocio responden con el código HTTP correspondiente (400, 403, 404, 409, 423, 503) y un cuerpo `{"mensaje": "..."}`.

Cuentas de ejemplo (clave `PagoFlex2026`): `crojas@correo.ejemplo` (persona), `alagos@pagoflex.ejemplo` (agente), `psaavedra@altosmaipo.ejemplo` (ejecutivo).

## Pruebas unitarias y cobertura

```
mvn clean verify
```

Ejecuta todas las pruebas (JUnit 5, Mockito y MockMvc) y JaCoCo. El build falla si la cobertura de líneas de un servicio baja de 80 %. Informe por servicio:

```
<servicio>/target/site/jacoco/index.html
```

JaCoCo excluye `model`, `dto`, `config` y la clase `*Application`, que no tienen lógica.

## Probar desde el celular o desde fuera de la red

La app usa `http://10.0.2.2:8080/` en el emulador (el equipo anfitrión). Para exponer el Gateway:

- VS Code: pestaña **Puertos** -> **Reenviar un puerto** -> `8080` -> visibilidad **Público**. Copia la URL `https://...devtunnels.ms/`.
- En la app cambia `BASE_URL` en `app/src/main/java/cl/duoc/pagoflex/data/red/ConfiguracionRed.kt` y vuelve a compilar.
