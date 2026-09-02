# Límites de Subida de Documentos - Implementación

## 📋 Resumen

Se ha implementado un sistema de límites para controlar cuántos documentos de cada tipo puede subir un alumno:

- **Plantilla Semanal**: Máximo **6 subidas**
- **Otros tipos** (Contrato, Autorización, Ficha Final Alumno, Ficha Final Evaluativa): Máximo **1 subida** cada uno

---

## 🔧 Archivos Creados

### 1. `LimitesDocumentoService.java` (NUEVO)
**Ubicación:** `src/main/java/com/EscuelaEmpresa/gestor_pasantes/service/`

Servicio que gestiona:
- Obtener límite para cada tipo de documento
- Validar si un alumno puede subir más documentos
- Generar mensajes de error descriptivos
- Contar documentos subidos por tipo

**Métodos principales:**
```java
obtenerLimitePorTipo(TipoDocumento tipo)          // Retorna 1 o 6
contarDocumentosSubidos(Integer idAlumno, TipoDocumento tipo)
puedeSubirDocumento(Integer idAlumno, TipoDocumento tipo)  // true/false
obtenerMensajeError(TipoDocumento tipo)           // Mensaje si alcanzó límite
obtenerMensajeInfo(Integer idAlumno, TipoDocumento tipo)   // Estado actual
```

---

## 🔄 Archivos Modificados

### 1. `DocumentoSubidoRepository.java`
- **Cambio:** Agregado método para contar documentos por tipo de alumno
  ```java
  long countByAlumno_IdAlAndTipoDocumento(Integer idAl, TipoDocumento tipoDocumento);
  ```

### 2. `SubirController.java`
- **Cambios:**
  - ✅ Inyectado `LimitesDocumentoService`
  - ✅ Modificado método `mostrarSubir()` para pasar límites al template
  - ✅ Agregada validación de límites en `subirDocumento()`
  - ✅ Rechazo con mensaje descriptivo si se alcanzó el límite

**En `mostrarSubir()`:**
- Calcula para cada tipo de documento:
  - Cuántos ya subió el alumno
  - Cuántos más puede subir
- Pasa esta información al template

**En `subirDocumento()`:**
```java
// Validar límites ANTES de procesar el archivo
if (!limitesDocumentoService.puedeSubirDocumento(alumno.getIdAl(), tipoDocumento)) {
    String mensaje = limitesDocumentoService.obtenerMensajeError(tipoDocumento);
    redirectAttributes.addFlashAttribute("error", mensaje);
    return "redirect:/alumno/subir";
}
```

### 3. `alumno/subir.html`
- **Cambios:**
  - ✅ Select con información de límites/subidos
  - ✅ Selector deshabilita opciones al alcanzar límite
  - ✅ JavaScript que muestra estado dinámico
  - ✅ Información visual: "✓ 2/6 documentos"

**Mejoras visuales:**
- Verde: Estado OK
- Naranja: Advertencia (casi alcanzó límite)
- Rojo: Límite alcanzado
- Botón deshabilitado cuando no puede subir

---

## 🎯 Flujo de Validación

### 1. Alumno accede a "Subir"
```
1. GET /alumno/subir
2. SubirController.mostrarSubir()
   - Obtiene documentos del alumno
   - Calcula: limites y subidos actuales
   - Pasa al template
3. Template muestra:
   - Select con: "Plantilla Semanal (2/6)"
   - Estado debajo: "✓ Has subido 2 de 6 (4 restantes)"
```

### 2. Alumno selecciona tipo de documento
```
1. Selecciona en el dropdown
2. JavaScript actualiza dinámicamente:
   - Estado de límite
   - Color del indicador
   - Estado del botón (enabled/disabled)
```

### 3. Alumno intenta subir documento
```
1. POST /alumno/subir
2. SubirController.subirDocumento()
   a) Validar archivo no vacío
   b) Validar tipo seleccionado
   c) Validar que PUEDE subir este tipo
      - Si NO → Error: "Ya subiste el máximo"
      - Si SÍ → Continuar
   d) Validar PDF válido
   e) Calcular hash SHA-256
   f) Guardar en BD
3. Redirect con éxito/error
```

---

## 📊 Ejemplos de Mensajes

### Cuando alcanza límite
```
Plantilla Semanal (6 subidas máximo):
"Ya alcanzaste el límite de 6 documentos de tipo 'Plantilla Semanal'."

Contrato (1 subida máximo):
"Ya subiste el máximo de 1 documento de tipo 'Contrato de Pasantía'. 
Para subir otro, debes eliminar el anterior."
```

### Interfaz en tiempo real
```
┌────────────────────────────────────┐
│ Tipo de documento *                │
│ [Plantilla Semanal                ▼] │
│                                    │
│ ✓ Has subido 2 de 6 (4 restantes)  │
│                                    │
│ ⚠️ Has subido 5 de 6 (1 restante)   │
│                                    │
│ ❌ Ya alcanzaste el máximo de 6.   │
│    [Subir documento] ← DESHABILITADO│
└────────────────────────────────────┘
```

---

## 🔐 Seguridad

### Validación del lado del servidor
- ✅ La validación ocurre en el controlador **antes** de procesar
- ✅ Protegido contra manipulación de JavaScript
- ✅ Conteo desde BD es confiable

### Validación del lado del cliente
- ✅ Interfaz intuitiva y responsiva
- ✅ Desactiva botón cuando no puede subir
- ✅ Muestra información clara

---

## 🧪 Casos de Prueba

### Test 1: Plantilla Semanal (6 máximo)
```
1. Subir 1ª Plantilla → OK
2. Subir 2ª Plantilla → OK
3. Subir 3ª Plantilla → OK
...
6. Subir 6ª Plantilla → OK
7. Intentar subir 7ª → ERROR: "Ya alcanzaste el límite de 6"
8. Eliminar una → OK
9. Subir 7ª → OK
```

### Test 2: Contrato (1 máximo)
```
1. Subir Contrato → OK
2. Intentar subir otro Contrato → ERROR: "Ya subiste el máximo de 1"
3. Eliminar → OK
4. Subir nuevo → OK
```

### Test 3: Interface dinámmica
```
1. Abrir página
2. Select muestra: "Contrato (1/1)" - disponible
3. Subir Contrato
4. Refresh
5. Select muestra: "Contrato (1/1)" - DESHABILITADO
6. No se puede seleccionar
```

---

## 📝 Información Técnica

### BD - Consultas útiles

```sql
-- Ver cuántos documentos de cada tipo subió un alumno
SELECT tipo_documento, COUNT(*) as cantidad
FROM documento_scan
WHERE id_Al = 123
GROUP BY tipo_documento;

-- Encontrar alumnos que ya no pueden subir Plantilla Semanal
SELECT DISTINCT ds.id_Al, COUNT(*) as plantillas
FROM documento_scan ds
WHERE ds.tipo_documento = 'PLANTILLA_SEMANAL'
GROUP BY ds.id_Al
HAVING COUNT(*) >= 6;
```

### Java - Verificar límites programáticamente

```java
@Autowired
private LimitesDocumentoService limitesService;

// En un admin panel o reporte
for (Alumno alumno : alumnos) {
    long plantillas = limitesService.contarDocumentosSubidos(
        alumno.getIdAl(), 
        TipoDocumento.PLANTILLA_SEMANAL
    );
    
    if (plantillas >= 6) {
        System.out.println("Alumno " + alumno.getNombre() + 
                          " alcanzó límite de Plantillas");
    }
}
```

---

## 🎨 Estilos CSS (Sugerido)

Para mejorar la visualización, considera agregar a `styles.css`:

```css
.info-limite {
    margin: 0.5rem 0;
    font-size: 0.85rem;
    padding: 0.5rem;
    border-radius: 4px;
    background-color: rgba(46, 213, 115, 0.1);
    color: #27ae60;
}

.info-limite:empty {
    display: none;
}

#tipo-documento:disabled,
#tipo-documento option:disabled {
    background-color: #ecf0f1;
    color: #95a5a6;
    cursor: not-allowed;
}

#btn-subir:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
```

---

## ✅ Testing Manual

Pasos recomendados:

1. **Compilar y ejecutar:**
   ```bash
   .\mvnw.cmd clean compile -q
   .\mvnw.cmd spring-boot:run
   ```

2. **Abrir navegador:**
   - http://localhost:8080
   - Login como alumno

3. **Test Plantilla Semanal:**
   - Ir a "Subir"
   - Ver que muestra: "Plantilla Semanal (0/6)"
   - Subir 6 PDFs
   - Verify que en la 7ª muestra error

4. **Test Contrato:**
   - Seleccionar "Contrato de Pasantía"
   - Subir 1 documento
   - Verificar que se desactiva
   - Eliminar documento
   - Verificar que se reactiva

5. **Test Interfaz Dinámica:**
   - Cambiar entre tipos
   - Ver que actualiza estado
   - Verificar colores (verde/naranja/rojo)

---

## 🔄 Cambios Futuros Posibles

- Permitir al admin configurar límites desde UI
- Hacer excepciones por alumno
- Historial de subidas y eliminaciones
- Notificaciones cuando está cerca del límite
- Reportes de alumnos por límite alcanzado

---

## 📦 Resumen de Cambios

| Archivo | Tipo | Cambios |
|---------|------|---------|
| `LimitesDocumentoService.java` | NUEVO | +180 líneas |
| `DocumentoSubidoRepository.java` | MOD | +1 método |
| `SubirController.java` | MOD | +15 líneas (validación) |
| `alumno/subir.html` | MOD | +50 líneas (JS + indicador) |

**Total:** 1 archivo nuevo + 3 modificados

---

**Compilación:** ✅ EXITOSA  
**Status:** ✅ LISTO PARA USAR
