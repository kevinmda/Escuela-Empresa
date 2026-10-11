# Opciones de Hosting para Gestor Pasantes

## Contexto del Proyecto
- **Tipo**: Aplicación Spring Boot (Java)
- **Framework**: Spring Boot 4.1.0
- **Base de datos**: MySQL 8.4
- **Deployment**: Docker Compose
- **Puerto**: 8080
- **Características especiales**: 
  - Soporte para JWT (API móvil Android)
  - Manejo de uploads (planillas)
  - Autenticación con Spring Security

## Problema Actual
El contenedor Docker no funciona correctamente. Se necesita hostear el servidor en una plataforma confiable.

---

## Opciones de Hosting Evaluadas

### ❌ **Vercel - NO RECOMENDADO**
**Por qué no:**
- Optimizado solo para frontend (Next.js, React, Vue)
- Soporta serverless functions con máximo 15 minutos de ejecución
- No soporta Docker Compose
- No es adecuado para aplicaciones backend monolíticas
- No mantiene procesos corriendo constantemente

---

### ✅ **Railway - RECOMENDADO**
**Pros:**
- ⭐ Soporta Docker Compose nativamente
- ⭐ Detecta `docker-compose.yml` automáticamente
- ⭐ MySQL integrado fácilmente
- ⭐ Deploy en 2-3 clics desde GitHub
- Variables de entorno simples de gestionar
- $5 USD/mes de crédito gratuito
- Perfecto para equipos pequeños

**Cons:**
- Después del crédito, costo puede aumentar
- Menos documentación que algunas alternativas

**Cómo deployar:**
1. Conectar repositorio GitHub a Railway
2. Railway detecta `docker-compose.yml`
3. Configurar variables de entorno (`.env`)
4. Deploy automático en cada push

---

### ✅ **Render**
**Pros:**
- Soporta Docker
- Interfaz amigable
- Documentación buena
- Desde $7 USD/mes

**Cons:**
- Plan gratuito más limitado que Railway
- Requiere más configuración manual

---

### ✅ **Fly.io**
**Pros:**
- Excelente rendimiento
- Soporte global con edge
- Generoso plan gratuito
- Muy rápido

**Cons:**
- Curva de aprendizaje más pronunciada
- Require CLI `flyctl`

---

### ✅ **Alternativas Tradicionales**
- **DigitalOcean**: $6 USD/mes (VPS completo)
- **AWS EC2**: Mayor control, mais complejo
- **Linode**: $5 USD/mes, buena documentación

---

## Mi Recomendación: Railway

**Razón:** Es la opción más fácil y directa para tu caso específico.

### Pasos para deployar en Railway:
1. **Preparar repositorio:**
   ```bash
   # Asegurarse que el Dockerfile y docker-compose.yml estén commiteados
   git add Dockerfile docker-compose.yml
   git commit -m "Add Docker configuration for Railway deployment"
   ```

2. **En Railway.app:**
   - Crear cuenta (con GitHub es fácil)
   - New Project → GitHub Repo
   - Seleccionar tu repositorio
   - Railway detecta `docker-compose.yml`
   - Configurar variables de entorno desde `.env.example`

3. **Variables de entorno necesarias:**
   ```
   MYSQL_ROOT_PASSWORD=tu_password_seguro
   MAIL_USERNAME=tu_email
   MAIL_PASSWORD=tu_app_password
   REMEMBER_ME_KEY=clave_aleatoria
   JWT_SECRET=tu_secret_jwt
   APP_URL_BASE=https://tu-dominio.railway.app
   COOKIES_SEGURAS=true
   APP_SECURITY_FORZAR_HTTPS=true
   ```

---

## Próximos Pasos
1. **Primero:** Debuggear el error específico del contenedor
   ```bash
   docker-compose logs
   ```
2. **Luego:** Elegir Railway o la alternativa que prefieras
3. **Finalmente:** Configurar el deploy

---

## Preguntas Pendientes
- ¿Cuál es el error exacto que da `docker-compose logs`?
- ¿Necesitas soporte para CI/CD automático?
- ¿Qué presupuesto mensual tienes para hosting?
