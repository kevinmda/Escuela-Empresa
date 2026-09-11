# Gestor de Pasantes — Colegio Técnico Nacional

Aplicación web para administrar las pasantías de 6 semanas de los alumnos del CTN:
planillas de control semanal, documentos escaneados, formularios oficiales del PEL
prellenados en PDF, informe final en Word, y la gestión de empresas y supervisores.

**Stack:** Java 17 · Spring Boot 4.1 (MVC, Security, Data JPA, Mail) · Thymeleaf ·
MySQL · PDFBox · Apache POI. Sin frameworks de frontend: CSS y JS propios.

## Roles

| Rol | Se define por | Qué hace |
|---|---|---|
| **Alumno** | fila en `alumno` | Carga hasta 6 planillas semanales, sube PDFs, imprime contrato / autorización / fichas con sus datos, descarga el informe final (`.docx`) cuando tiene las 6 semanas, empresa y supervisor. |
| **Administrativo** | fila en `administrador` con `cargo = 'administrativo'` | Ve todos los alumnos del colegio (filtro especialidad → curso → sección, búsqueda), sus documentos y reportes de entregas; descarga ZIPs. |
| **Coordinador** | fila en `administrador` con `cargo = 'coordinador'` y una `especialidad` que lo apunta | Lo mismo, acotado a su especialidad, y además da de alta empresas y supervisores y los asigna a los alumnos. |

Administrativo y coordinador comparten `ROLE_ADMIN`; alumno es `ROLE_ALUMNO`. El rol se
resuelve en `CustomUserDetailsService`.

## Puesta en marcha

1. **MySQL** (XAMPP sirve). Instalación nueva:
   ```bash
   mysql -u root -p < db/schema.sql
   ```
   Base ya existente: aplicá lo que falte de `db/migraciones/` (están fechados).
2. **Configuración**: copiá `src/main/resources/application.properties.example` a
   `application.properties` y completá usuario/clave de MySQL. El archivo real está
   en `.gitignore`.
3. **Correo**: definí `MAIL_USERNAME` y `MAIL_PASSWORD` (contraseña de aplicación de
   Gmail) como variables de entorno o en el properties. Sin esto el primer ingreso de
   una cuenta nueva falla al mandar el código.
4. **Correr**:
   ```bash
   ./mvnw spring-boot:run
   ```
   Abre en http://localhost:8080. En Windows, `mvnw.cmd`.

### Primer usuario

No hay pantalla de alta: el primer administrativo se inserta a mano (ver el final de
`db/schema.sql`). La contraseña va como hash BCrypt. Para generar uno con las mismas
librerías del proyecto:

```bash
./mvnw -q dependency:build-classpath -Dmdep.outputFile=cp.txt
jshell --class-path "$(cat cp.txt)"
```
```java
new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("la-clave")
```

Las cuentas nacen con `activo = FALSE` y `contrasena_por_defecto = TRUE`: en el primer
login correcto se manda un código de 6 dígitos al email, y al entrar se obliga a
cambiar la contraseña.

## Cómo funciona

**Acceso.** Login en dos pasos (email → contraseña), misma pantalla exista o no la
cuenta. 5 intentos fallidos bloquean 15 minutos (contadores atómicos en la BD).
Recuperación de contraseña por código de 5 minutos. BCrypt, CSRF activo, remember-me
de 14 días (fijá `REMEMBER_ME_KEY` en producción).

**Planilla semanal** (`/alumno/planilla`). Seis filas lunes–sábado; cada día cargado
necesita fecha, descripción (≤ 65) y horas (> 0, dos decimales). Las fechas tienen
que coincidir con el día de la fila, ir en orden, caber en una semana y no solaparse
con otra planilla. Máximo 6 planillas. Cada una se puede ver como PDF sobre la
plantilla oficial `CONTROL_SEMANAL_PEL_2025.pdf`.

**Documentos** (`/alumno/subir`). Solo PDF (se verifica el `%PDF` inicial), hasta
10 MB, con SHA-256 guardado. Límites por tipo:

| Tipo | Máximo |
|---|---|
| `PLANTILLA_SEMANAL` | 6 |
| `CONTRATO`, `AUTORIZACION`, `FICHA_FINAL_ALUMNO`, `FICHA_FINAL_EVALUATIVA` | 1 cada uno |

Los archivos se guardan con nombre UUID en `app.uploads.directorio` (no se
versionan; `uploads/` está en `.gitignore`). El alumno solo ve y borra los propios;
el admin/coordinador ve los de su alcance.

**Formularios** (`/alumno/imprimir`). Contrato, autorización de padres y las dos
fichas finales, prellenados con PDFBox sobre las plantillas de
`src/main/resources/plantillas/` (se leen del classpath, así que funcionan desde el
JAR). El informe final se arma con POI sobre `Informe_Pasantia_Plantilla.docx`
usando los bookmarks del documento.

**Errores.** `ManejoErroresGlobal` convierte cada excepción en una página
(`templates/error.html`) con el código correcto; las rutas `/admin/api/**`
reciben JSON. `RecursoNoEncontradoException` → 404, `ReglaNegocioException` → 400
con el mensaje, el resto → 500 con log.

## Estructura

```
src/main/java/.../gestor_pasantes/
  config/        SecurityConfig, handlers de login, interceptor de contraseña por defecto
  controller/    uno por pantalla + ChromeModelAdvice (header) + ManejoErroresGlobal
  service/       planillas, PDF, informe DOCX, validación y límites de documentos, plantillas, mail
  repository/    Spring Data JPA
  entity/  dto/  exception/  util/
src/main/resources/
  templates/     Thymeleaf (fragments/chrome.html es el header común)
  static/        css/styles.css, js/theme.js, img/, pdf/ (manuales por rol)
  plantillas/    PDFs oficiales del PEL y la plantilla del informe
db/              schema.sql (instalación nueva) y migraciones/ (cambios fechados)
```

## Tests

```bash
./mvnw test
```

`GestorPasantesApplicationTests.contextLoads` necesita MySQL levantado. Los demás
(errores, plantillas, descargas, suma de horas) corren solos:

```bash
./mvnw test -Dtest='DescargaTest,PlanillaSemanalServiceTest,PlantillaServiceTest,ManejoErroresGlobalTest'
```

## Despliegue

`./mvnw package` genera `target/gestor-pasantes-0.0.1-SNAPSHOT.jar`. En el servidor
hacen falta: MySQL con el esquema, `application.properties` al lado del JAR (o las
propiedades por variables de entorno), `MAIL_*`, `REMEMBER_ME_KEY`, y un
`app.uploads.directorio` absoluto con permisos de escritura.
