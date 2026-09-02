# Checklist de Instalación e Implementación

## ✓ Desarrollo Completado

- [x] Crear enum `TipoDocumento` con 5 tipos de documentos
- [x] Modificar entidad `DocumentoSubido` con nuevos campos
- [x] Crear servicio `ValidacionDocumentoService` con métodos de seguridad
- [x] Modificar controlador `SubirController` con validación completa
- [x] Actualizar DTO `DocumentoSubidoAdminDTO` para nuevos campos
- [x] Modificar template `alumno/subir.html` con selector de tipos
- [x] Actualizar `AdminController` para mostrar nuevos datos
- [x] Crear migración SQL para actualizar BD
- [x] Compilación exitosa sin errores

## Archivos a Aplicar/Revisar

### 1. Verificar Archivos Nuevos Creados

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/entity/TipoDocumento.java`
  - Ubicación correcta: ✓
  - Compilar sin errores: ✓
  - Enum definido correctamente: ✓

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/service/ValidacionDocumentoService.java`
  - Ubicación correcta: ✓
  - Compilar sin errores: ✓
  - Métodos de seguridad implementados: ✓

### 2. Verificar Archivos Modificados

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/entity/DocumentoSubido.java`
  - Campos nuevos agregados: ✓
  - Getters/Setters agregados: ✓
  - Imports correctos: ✓

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/controller/SubirController.java`
  - Inyección de servicio de validación: ✓
  - Método subirDocumento() reescrito: ✓
  - Manejo de tipoDocumento: ✓
  - Cálculo de hash: ✓

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/controller/AdminController.java`
  - Método documentosSubidosPorAlumno() actualizado: ✓
  - Nuevos parámetros en DTO: ✓

- [ ] `src/main/java/com/EscuelaEmpresa/gestor_pasantes/dto/DocumentoSubidoAdminDTO.java`
  - Campos nuevos agregados: ✓
  - Constructores actualizados: ✓
  - Getters para nuevos campos: ✓

- [ ] `src/main/resources/templates/alumno/subir.html`
  - Selector de tipo de documento: ✓
  - Tabla actualizada con tipo: ✓
  - Validación requerida: ✓

### 3. Base de Datos

- [ ] Script SQL disponible: `src/main/resources/db/migration/V1_0__agregar_tipos_documento.sql`
  - 3 columnas nuevas definidas: ✓
  - 2 índices creados: ✓
  - Sentencias SQL correctas: ✓

## Pasos de Implementación

### Paso 1: Compilación y Testing Local
```bash
# En la carpeta del proyecto
.\mvnw.cmd clean compile -q

# Debe completar sin errores
✓ Success
```

### Paso 2: Actualizar Base de Datos (IMPORTANTE)
```bash
# Opción A: Si usas Flyway/Liquibase automático
# La migración se ejecutará al iniciar la aplicación

# Opción B: Manual (ejecutar en tu BD)
# Conectarte a tu BD y ejecutar:
# src/main/resources/db/migration/V1_0__agregar_tipos_documento.sql

# Verificar que se ejecutó correctamente:
# - Tabla documento_scan debe tener 3 columnas nuevas
# - 2 índices deben estar creados
```

**Comando para verificar (MySQL):**
```sql
DESCRIBE documento_scan;
-- Debe mostrar: tipo_documento, hash_integridad, validado

SHOW INDEXES FROM documento_scan;
-- Debe mostrar: idx_documento_scan_tipo, idx_documento_scan_validado
```

### Paso 3: Build del Proyecto
```bash
# Compilar y empaquetar
.\mvnw.cmd clean package -q

# O sin tests si necesitas ir rápido
.\mvnw.cmd clean package -DskipTests -q
```

### Paso 4: Iniciar la Aplicación
```bash
# Opción A: Desde IDE (Spring Boot Run)
# Click en botón Play en tu IDE

# Opción B: Desde línea de comandos
.\mvnw.cmd spring-boot:run

# Opción C: Jar compilado
java -jar target/gestor_pasantes-0.0.1-SNAPSHOT.jar
```

### Paso 5: Verificación Básica
1. [ ] Abrir navegador en http://localhost:8080
2. [ ] Loguear como alumno
3. [ ] Ir a "Subir"
4. [ ] Verificar que aparece selector de tipo de documento
5. [ ] Intentar subir sin seleccionar tipo → Debe dar error
6. [ ] Seleccionar tipo y subir PDF válido → Debe funcionar
7. [ ] Ver el documento en tabla → Debe mostrar tipo

### Paso 6: Testing Admin
1. [ ] Loguear como administrador
2. [ ] Ir a "Alumnos"
3. [ ] Ver documentos de un alumno
4. [ ] Verificar que muestra tipo de documento
5. [ ] Verificar que muestra si está validado
6. [ ] Descargar documento → Debe funcionar

## Pruebas Recomendadas

### Test 1: Upload PDF Válido
```
ENTRADA:
- Tipo: "CONTRATO"
- Archivo: contrato.pdf (válido)

ESPERADO:
- ✓ "Documento subido correctamente"
- ✓ Aparece en tabla con tipo correcto
- ✓ validado = true en BD
- ✓ hash_integridad tiene valor SHA-256
```

### Test 2: Upload sin Tipo
```
ENTRADA:
- Tipo: (ninguno)
- Archivo: documento.pdf

ESPERADO:
- ✗ "Seleccioná un tipo de documento"
- ✗ No se guarda en BD
```

### Test 3: Upload Archivo Falso
```
ENTRADA:
- Tipo: "PLANTILLA_SEMANAL"
- Archivo: imagen.jpg renombrada a .pdf

ESPERADO:
- ✗ "El archivo no es un PDF válido"
- ✗ No se guarda en BD
```

### Test 4: Upload Muy Grande
```
ENTRADA:
- Tipo: "AUTORIZACION"
- Archivo: archivo_100mb.pdf

ESPERADO:
- ✗ "El archivo excede el tamaño máximo permitido (50 MB)"
- ✗ No se guarda en BD
```

### Test 5: Verificar Integridad
```
ENTRADA:
- Descargar documento subido
- Calcular SHA-256 del archivo local

ESPERADO:
- Hash local == hash guardado en BD
- ✓ Documento íntegro
```

## Troubleshooting

### Problema: "TipoDocumento cannot be resolved"
**Solución:**
- Verificar que el archivo `TipoDocumento.java` está en la ubicación correcta
- Clean + Rebuild del proyecto
- Maven: `.\mvnw.cmd clean install -q`

### Problema: "ValidacionDocumentoService cannot be resolved"
**Solución:**
- Verificar que el archivo está en la carpeta `service`
- La anotación `@Service` debe estar presente
- Clean + Rebuild

### Problema: "Column tipo_documento doesn't exist"
**Solución:**
- Ejecutar el script de migración SQL
- Verificar que la BD se actualizó correctamente
- Verificar nombre de la columna (tipo_documento, NO tipo_doc)

### Problema: "Application no inicia después de cambios"
**Solución:**
- Verificar errores de compilación: `.\mvnw.cmd compile -q`
- Verificar que servicios están anotados con `@Service`
- Verificar que inyecciones están correctas
- Ver logs en consola

### Problema: "Selector no aparece en HTML"
**Solución:**
- Verificar que Thymeleaf procesó el template
- Verificar que `tiposDocumento` se pasó en el Model (en SubirController)
- Limpiar caché del navegador (Ctrl+Shift+Delete)

## Rollback (si es necesario)

### Volver a versión anterior
```sql
-- Eliminar las nuevas columnas
ALTER TABLE documento_scan DROP COLUMN tipo_documento;
ALTER TABLE documento_scan DROP COLUMN hash_integridad;
ALTER TABLE documento_scan DROP COLUMN validado;

-- Eliminar los nuevos índices
DROP INDEX idx_documento_scan_tipo ON documento_scan;
DROP INDEX idx_documento_scan_validado ON documento_scan;
```

### Revertir código Java
- Revert commits Git o restaurar versión anterior
- Eliminar archivos nuevos:
  - `TipoDocumento.java`
  - `ValidacionDocumentoService.java`
- Restaurar archivos a versión anterior

## Verificación Final

Antes de ir a producción, confirmar:

- [ ] Compilación sin errores: `.\mvnw.cmd clean compile -q`
- [ ] Tests pasan (si existen): `.\mvnw.cmd test -q`
- [ ] BD actualizada con migración
- [ ] Aplicación inicia sin errores
- [ ] Alumno puede subir documento con tipo
- [ ] Admin ve tipo y validado en lista
- [ ] Hash se calcula y almacena correctamente
- [ ] Documentos anteriores siguen siendo accesibles
- [ ] Control de acceso funciona (alumnos solo ven sus docs)
- [ ] Validaciones funcionan correctamente

## Documentación Generada

Revisar los siguientes archivos para más información:

1. **`IMPLEMENTACION_TIPOS_DOCUMENTOS.md`**
   - Documentación técnica completa
   - Detalles de cada componente
   - Métodos de seguridad
   - Mejoras futuras

2. **`GUIA_USO_TIPOS_DOCUMENTOS.md`**
   - Guía para alumnos
   - Guía para administradores
   - Casos de uso
   - FAQ

3. **`RESUMEN_TECNICO.md`**
   - Cambios por archivo
   - Flujo de datos
   - Performance
   - Testing recomendado

## Contacto/Soporte

Si hay problemas durante la implementación:

1. Revisar los archivos de documentación
2. Verificar errores de compilación
3. Revisar logs de la aplicación
4. Verificar que la BD se actualizó correctamente
5. Probar con casos simples primero

## Timeline Estimado

- Compilación: 5-10 segundos
- Migración BD: <1 segundo
- Instalación completa: <5 minutos
- Testing manual: 15-30 minutos
- Despliegue: <5 minutos (sin downtime si usas load balancer)

---

**Última actualización:** 2026-09-01
**Versión de implementación:** 1.0
**Status:** ✓ Completo y Compilado
