# Guía de Uso: Sistema de Tipos de Documentos

## Para Alumnos

### Cómo Subir un Documento

1. **Acceder a "Subir" en el menú**
   - Desde el panel de alumno, click en "Subir" en la navegación

2. **Seleccionar el tipo de documento**
   ```
   Tipo de documento *
   [-- Seleccioná un tipo --]
   - Contrato de Pasantía
   - Plantilla Semanal
   - Autorización
   - Ficha Final del Alumno
   - Ficha Final Evaluativa
   ```
   - Es **obligatorio** seleccionar un tipo
   - Si no seleccionas uno, verás un error

3. **Seleccionar el archivo PDF**
   - Click en el área o arrastra tu PDF
   - El archivo debe ser PDF válido
   - Tamaño máximo: 50 MB

4. **Hacer click en "Subir documento"**
   - El sistema validará automáticamente el PDF
   - Se calculará un hash de seguridad (SHA-256)
   - Verás confirmación cuando se suba correctamente

5. **Ver el historial**
   - En "Mis documentos subidos" verás:
     - **Tipo**: El tipo que seleccionaste
     - **Nombre**: El nombre del archivo original
     - **Fecha**: Cuándo lo subiste
     - **Ver**: Abre el PDF
     - **Eliminar**: Borra el documento

### Ejemplo de Interfaz

```
┌─────────────────────────────────────────────────┐
│ Subir documento                                 │
├─────────────────────────────────────────────────┤
│ Tipo de documento *                             │
│ [Seleccioná un tipo                          ▼] │
│                                                 │
│ ┌─────────────────────────────────────────────┐ │
│ │ Hacé click o arrastrá tu PDF acá           │ │
│ │ Solo archivos PDF                           │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ [Subir documento]                               │
└─────────────────────────────────────────────────┘

Mis documentos subidos

| Tipo                    | Nombre       | Fecha           | Ver | Elim |
|-------------------------|--------------|-----------------|-----|------|
| Contrato de Pasantía    | contrato.pdf | 25/08/2026 14:30| ✓   | ✗    |
| Plantilla Semanal       | semana1.pdf  | 25/08/2026 15:45| ✓   | ✗    |
```

## Para Administradores

### Ver Documentos de un Alumno

Cuando visualizas un alumno específico, ahora ves:

```
Documentos Subidos:

[Tipo]                   [Nombre]      [Fecha]         [Validado] [Acción]
- Contrato de Pasantía   contrato.pdf  25/08/2026      ✓ Sí       Ver
- Plantilla Semanal      semana1.pdf   26/08/2026      ✓ Sí       Ver
- Autorización           autorizacion  27/08/2026      ✓ Sí       Ver
```

### Cómo Saber si un Documento es Seguro

**Campo "Validado":**
- ✓ **Sí**: El documento pasó todas las validaciones de seguridad:
  - Es un PDF válido (no corrupto)
  - Fue escaneado y se confirma su integridad
  - Tiene hash SHA-256 para auditoría

- ✗ **No**: El documento podría tener problemas:
  - Revisar manualmente
  - Preguntar al alumno que lo re-envíe

### Verificar Integridad de un Documento

**Desde la Base de Datos:**

```sql
-- Ver todos los documentos de un alumno con su estado
SELECT 
    id_DS,
    nombre,
    tipo_documento,
    fecha_subida,
    validado,
    hash_integridad
FROM documento_scan
WHERE id_Al = 123
ORDER BY fecha_subida DESC;

-- Encontrar documentos que no fueron validados
SELECT id_DS, nombre, tipo_documento, fecha_subida
FROM documento_scan
WHERE validado = false
ORDER BY fecha_subida DESC;
```

**Usando el Código Java:**

```java
// En AdminController u otro servicio
@Autowired
private ValidacionDocumentoService validacionService;
@Autowired
private DocumentoSubidoRepository docRepository;

public void verificarDocumento(Integer idDocumento) {
    DocumentoSubido doc = docRepository.findById(idDocumento).orElse(null);
    if (doc == null) return;
    
    File archivo = new File(doc.getRutaArchivo());
    
    // Verificar que el archivo no fue modificado
    boolean integro = validacionService.validarIntegridadArchivo(
        archivo, 
        doc.getHashIntegridad()
    );
    
    if (integro) {
        System.out.println("✓ Documento íntegro - No fue modificado");
    } else {
        System.out.println("✗ ALERTA: Documento fue modificado después de subir");
        // Aquí puedes enviar alertas, emails, etc.
    }
}
```

## Métodos de Seguridad Explicados

### 1. Validación de PDF
```
Archivo PDF válido debe:
✓ Iniciar con bytes: 0x25 0x50 0x44 0x46 ("%PDF")
✓ Tener type MIME: application/pdf
✓ Extensión: .pdf
✓ No estar corrupto
```

### 2. Hash SHA-256
```
Cada archivo tiene un "huella digital":
- Se calcula al momento de subir
- Se almacena en la BD
- Permite detectar si fue modificado después

Ejemplo:
Original: a3f8e9c2b1d7f4a8...c9b2e1a7
Posterior: a3f8e9c2b1d7f4a8...c9b2e1a7  ✓ Igual - OK
Posterior: x3f8e9c2b1d7f4a8...c9b2e1a7  ✗ Diferente - ALERTA
```

### 3. Validación de Tamaño
```
Máximo 50 MB por documento
- Evita que se saturen servidores
- Evita uploads de archivos innecesariamente grandes
```

### 4. Almacenamiento Seguro
```
Archivo guardado como:
/uploads/550e8400-e29b-41d4-a716-446655440000.pdf

No como:
/uploads/contrato-facundo.pdf

Beneficios:
- No se puede adivinar rutas
- No hay información sensible en nombres
- Previene directory traversal attacks
```

### 5. Control de Acceso
```
Un alumno SOLO puede:
- Ver sus propios documentos
- Eliminar sus propios documentos

No puede:
- Ver documentos de otros alumnos
- Modificar documentos
- Acceder a rutas diferentes

El sistema verifica:
- Autenticación (¿Quién eres?)
- Autorización (¿Qué puedes ver?)
```

## Casos de Uso Comunes

### Caso 1: Alumno Sube Contrato

```
1. Alumno abre "Subir"
2. Selecciona "Contrato de Pasantía"
3. Arrastra el archivo contrato.pdf
4. Click en "Subir documento"

Sistema realiza:
- ✓ Verifica que sea PDF válido
- ✓ Calcula hash SHA-256
- ✓ Guarda con nombre único (UUID.pdf)
- ✓ Almacena en BD con tipo CONTRATO y validado=true
- ✓ Muestra "Documento subido correctamente"

Admin ve:
- Tipo: Contrato de Pasantía
- Validado: Sí
- Puede descargar con confianza
```

### Caso 2: Alumno Intenta Subir Archivo Falso

```
1. Alumno renombra un .docx a .pdf
2. Intenta subir

Sistema:
- ✗ Detecta que no es PDF real (magic numbers incorrectos)
- ✗ Rechaza con: "El archivo no es un PDF válido"
- ✗ No se guarda en BD
- Alumno debe subir PDF real
```

### Caso 3: Admin Verifica Integridad

```
1. Admin descarga documento de hace 2 meses
2. Ejecuta verificación de hash

Sistema:
- Compara hash actual con hash guardado
- Si son iguales: "✓ Documento íntegro"
- Si son diferentes: "✗ ALERTA: Documento fue modificado"
```

## Preguntas Frecuentes

**P: ¿Qué pasa si subo un archivo que no es PDF?**
R: El sistema lo rechaza automáticamente. Verás un mensaje: "El archivo no es un PDF válido".

**P: ¿Puedo cambiar el tipo de documento después de subir?**
R: No actualmente. Debes eliminar y re-subir con el tipo correcto.

**P: ¿Está protegido mi documento?**
R: Sí, tiene múltiples niveles:
- Validación de integridad (PDF válido)
- Hash SHA-256 para detectar modificaciones
- Control de acceso (solo tú puedes verlo)
- Almacenamiento seguro (nombre UUID)

**P: ¿Cuál es el tamaño máximo?**
R: 50 MB por archivo.

**P: ¿Qué tipos de documentos puedo subir?**
R: Solo estos 5:
1. Contrato de Pasantía
2. Plantilla Semanal
3. Autorización
4. Ficha Final del Alumno
5. Ficha Final Evaluativa

**P: ¿Dónde se guardan los archivos?**
R: En el servidor, en la ruta configurada en application.properties (no accesible directamente).

**P: ¿Puedo eliminar un documento después?**
R: Sí, cada documento tiene botón "Eliminar". Una vez eliminado, no se puede recuperar.
