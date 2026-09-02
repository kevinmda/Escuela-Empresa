# Implementación: Tipos de Documentos y Validación de Seguridad

## Resumen de Cambios

Se ha implementado un sistema completo de categorización y validación de documentos en el módulo de carga de alumnos.

## Cambios Realizados

### 1. **Base de Datos**
- **Archivo**: `src/main/resources/db/migration/V1_0__agregar_tipos_documento.sql`
- Se agregaron 3 nuevas columnas a la tabla `documento_scan`:
  - `tipo_documento`: Almacena el tipo de documento seleccionado (ENUM)
  - `hash_integridad`: Hash SHA-256 del archivo para verificar integridad posterior
  - `validado`: Flag booleano que indica si el documento pasó las validaciones
- Se crearon índices para optimizar búsquedas por tipo y validación

### 2. **Entidades Java**

#### Nuevo Enum: `TipoDocumento.java`
```java
public enum TipoDocumento {
    CONTRATO("Contrato de Pasantía"),
    PLANTILLA_SEMANAL("Plantilla Semanal"),
    AUTORIZACION("Autorización"),
    FICHA_FINAL_ALUMNO("Ficha Final del Alumno"),
    FICHA_FINAL_EVALUATIVA("Ficha Final Evaluativa");
}
```

#### Modificación: `DocumentoSubido.java`
- Agregados campos:
  - `TipoDocumento tipoDocumento`
  - `String hashIntegridad`
  - `Boolean validado`

### 3. **Servicios**

#### Nuevo Servicio: `ValidacionDocumentoService.java`
Proporciona métodos para validar documentos con seguridad automatizada:

**Métodos principales:**

1. **`validarPdfIntegridad(MultipartFile)`**
   - Verifica que el content-type sea PDF
   - Valida los magic numbers del PDF (debe comenzar con "%PDF")
   - Detecta archivos PDF corrupto

2. **`calcularSHA256(File)` y `calcularSHA256(Path)`**
   - Calcula el hash SHA-256 del archivo guardado
   - Permite verificar integridad posterior

3. **`calcularSHA256(MultipartFile)`**
   - Calcula el hash del archivo antes de guardarlo
   - Usado para validación completa durante la subida

4. **`validarIntegridadArchivo(File, String hashEsperado)`**
   - Verifica que el archivo guardado no haya sido modificado
   - Compara el hash actual con el hash guardado en BD

5. **`validarDocumentoCompleto(MultipartFile, long tamanioMaximo)`**
   - Realiza validación integral:
     - Verifica tamaño máximo (50 MB por defecto)
     - Valida que sea PDF válido
     - Calcula hash SHA-256
   - Retorna objeto `ValidacionResultado` con detalles

### 4. **Controladores**

#### Modificación: `SubirController.java`
- Inyectado: `ValidacionDocumentoService`
- Modificado método `mostrarSubir()`:
  - Pasa los tipos de documento disponibles al modelo
- Modificado método `subirDocumento()`:
  - Recibe parámetro `tipoDocumento` (requerido)
  - Valida que se haya seleccionado un tipo
  - Realiza validación completa del documento
  - Calcula y almacena hash SHA-256
  - Establece `validado = true` si pasa todas las validaciones

#### Modificación: `AdminController.java`
- Actualizado método `documentosSubidosPorAlumno()`:
  - Ahora envía tipo de documento y estado de validación en el DTO

### 5. **DTOs**

#### Modificación: `DocumentoSubidoAdminDTO.java`
- Agregados campos:
  - `TipoDocumento tipoDocumento`
  - `Boolean validado`
- Mantiene compatibilidad con constructor anterior

### 6. **Frontend**

#### Modificación: `templates/alumno/subir.html`
- Agregado selector de tipo de documento (requerido)
- Actualizada tabla de documentos para mostrar:
  - Tipo de documento
  - Nombre (igual que antes)
  - Fecha (igual que antes)
  - Enlaces Ver/Eliminar

## Métodos de Seguridad Implementados

### 1. **Validación de Integridad PDF**
- Los archivos deben ser PDFs válidos (validando magic numbers)
- Detecta intentos de upload de archivos falsos con extensión .pdf

### 2. **Hash SHA-256**
- Se calcula y almacena el hash SHA-256 de cada documento
- Permite verificar más tarde si el archivo fue modificado sin autorización
- Útil para auditoría y cumplimiento normativo

### 3. **Limite de Tamaño**
- Máximo 50 MB por documento (configurable)
- Previene uploads de archivos excesivamente grandes

### 4. **Validación de Tipo MIME**
- Verifica content-type del navegador
- Valida extensión del archivo
- Verifica firma/estructura interna del PDF

### 5. **Almacenamiento Seguro**
- Los archivos se guardan con nombres UUID (no guardables en directorios)
- Se validan permisos: un alumno solo puede ver/eliminar sus propios documentos
- Los archivos se guardan fuera del web root

### 6. **Flag de Validación**
- Campo `validado` indica si el documento pasó controles automáticos
- Permite a administradores saber cuáles documentos son confiables
- Facilita auditoría posterior

## Cómo Verificar Integridad de Documentos

### Desde el código (Java):
```java
// Para verificar que un documento guardado no fue modificado
String hashGuardado = documento.getHashIntegridad();
File archivo = new File(documento.getRutaArchivo());

boolean integro = validacionDocumentoService.validarIntegridadArchivo(
    archivo, 
    hashGuardado
);

if (!integro) {
    // El archivo fue modificado después de ser subido
    logger.warn("ALERTA: Documento modificado - ID: " + documento.getIdDs());
}
```

### Desde la BD (SQL):
```sql
-- Encontrar todos los documentos validados de un alumno
SELECT id_DS, nombre, tipo_documento, validado, hash_integridad
FROM documento_scan
WHERE id_Al = ? 
AND validado = true
ORDER BY fecha_subida DESC;

-- Encontrar documentos que podrían estar comprometidos
SELECT id_DS, nombre, tipo_documento
FROM documento_scan
WHERE validado = false;
```

## Pasos de Instalación

1. **Copiar archivos nuevos/modificados**
   - `src/main/java/com/EscuelaEmpresa/gestor_pasantes/entity/TipoDocumento.java`
   - `src/main/java/com/EscuelaEmpresa/gestor_pasantes/service/ValidacionDocumentoService.java`

2. **Ejecutar migración SQL**
   ```sql
   -- Ejecutar el contenido de src/main/resources/db/migration/V1_0__agregar_tipos_documento.sql
   ```

3. **Reconstruir la aplicación**
   ```bash
   mvn clean install
   ```

4. **Reiniciar la aplicación**
   ```bash
   # La aplicación detectará el nuevo servicio automáticamente (inyección de dependencias)
   ```

## Testing

### Pruebas manuales recomendadas:

1. **Subir un PDF válido**
   - Verificar que se muestre selector de tipo
   - Verificar que se requiera tipo
   - Verificar que aparezca en la tabla con tipo indicado

2. **Intentar subir un archivo que no sea PDF**
   - Debe ser rechazado con mensaje de error

3. **Intentar subir un archivo con extensión .pdf pero que no es PDF**
   - Debe ser rechazado (validación de magic numbers)

4. **Subir documento, descargarlo y verificar integridad**
   - Hash debe coincidir
   - `validado` debe ser `true`

5. **Como admin, ver documentos de un alumno**
   - Debe mostrar tipo de documento
   - Debe mostrar si fue validado

## Configuración (application.properties)

```properties
# Tamaño máximo de upload (en bytes)
# Por defecto: 50 MB
# El controlador usa TAMANIO_MAXIMO = 50 * 1024 * 1024
```

## Notas de Rendimiento

- Los índices en `tipo_documento` y `validado` mejoran consultas de admin
- El hash SHA-256 se calcula una sola vez durante upload
- No hay verificación automática periódica (se puede agregar con job de Spring)

## Mejoras Futuras Recomendadas

1. **Verificación periódica de integridad**
   - Crear un scheduled job que verifique hashes semanalmente
   - Alertar si algún documento fue modificado

2. **Firma digital**
   - Agregar firma digital usando certificados
   - Más seguro que solo hash

3. **Versionado de documentos**
   - Permitir múltiples versiones de mismo documento
   - Mantener historial de cambios

4. **Notificaciones**
   - Notificar a coordinador cuando alumno sube documento
   - Email confirmando recepción

5. **Escaneo de virus**
   - Integrar antivirus (ClamAV, etc.)
   - Escanear cada documento antes de guardar

6. **OCR/Validación de contenido**
   - Validar que el PDF contiene ciertos campos esperados
   - Detectar si el documento fue alterado significativamente

## Preguntas Frecuentes

**P: ¿Qué pasa con los documentos subidos antes de esta migración?**
R: Se establece `validado = true` automáticamente para mantener compatibilidad. En producción podrías revisar manualmente.

**P: ¿Puede el alumno cambiar el tipo de documento después de subir?**
R: No, actualmente debe eliminar y re-subir. Podrías agregar un edit endpoint si lo necesitas.

**P: ¿Dónde se almacenan los archivos?**
R: En la ruta configurada en `${app.uploads.directorio}` (en application.properties).

**P: ¿Qué pasa si alguien intenta modificar manualmente el hash en BD?**
R: El admin puede recalcular hashes usando la función `calcularSHA256()` del servicio y comparar.
