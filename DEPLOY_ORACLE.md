# Deploy gratis en Oracle Cloud (Always Free) + Caddy (HTTPS)

Arquitectura: una VM Ubuntu con Docker. `docker-compose.yml` levanta
**caddy** (80/443, HTTPS automático) → **app** (Spring Boot, solo accesible desde
localhost:8080 y desde Caddy) → **mysql**. Los datos viven en volúmenes Docker
(`mysql-data`, `uploads-data`).

> Las condiciones del plan gratuito de Oracle cambian: confirmalas en
> https://www.oracle.com/cloud/free/ antes de registrarte.

## 1. Crear la VM
1. Registrarse en Oracle Cloud (pide tarjeta solo para verificar identidad). **La región
   "home" se elige acá y no se puede cambiar**; si falta capacidad ARM, reintentar más tarde.
2. Compute → Instances → Create instance:
   - Imagen: **Ubuntu 22.04/24.04**.
   - Shape: **Ampere (VM.Standard.A1.Flex)**, 2 OCPU / 12 GB alcanza de sobra
     (alternativa: `VM.Standard.E2.1.Micro`, 1 GB: funciona pero justo).
   - Subir/generar tu clave SSH pública y **guardar la clave privada**.
   - Volumen de arranque: 50 GB.
3. Anotar la **IP pública**. Conviene reservarla (Networking → Reserved public IPs)
   para que no cambie.

## 2. Abrir puertos 80 y 443
**En Oracle:** Networking → VCN → Subnet → Security List → Add Ingress Rules:
`0.0.0.0/0`, TCP, destino **80** y otra regla para **443**.

**En la VM** (las imágenes de Oracle traen iptables restrictivo):
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo apt-get install -y iptables-persistent && sudo netfilter-persistent save
```

## 3. Dominio gratis (DuckDNS)
1. Entrar a https://www.duckdns.org, crear un subdominio (ej. `escuela-empresa`) y
   apuntarlo a la IP pública de la VM.
2. Verificar: `nslookup escuela-empresa.duckdns.org` debe devolver esa IP.

## 4. Instalar Docker y traer el código
```bash
ssh -i ~/.ssh/tu_clave ubuntu@IP_PUBLICA
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker
git clone https://github.com/kevinmda/Escuela-Empresa.git ~/Escuela-Empresa
cd ~/Escuela-Empresa
```

## 5. Configurar `.env`
```bash
cp .env.example .env
chmod 600 .env
openssl rand -hex 16   # → MYSQL_ROOT_PASSWORD
openssl rand -hex 16   # → REMEMBER_ME_KEY
openssl rand -hex 32   # → JWT_SECRET
nano .env
```
Completar `DOMINIO` y `APP_URL_BASE` (`https://<tu-dominio>`), dejar
`COOKIES_SEGURAS=true` y `APP_SECURITY_FORZAR_HTTPS=true`, y poner el mail/clave de
aplicación de Gmail.

## 6. Levantar
```bash
docker compose up -d --build
docker compose ps
docker compose logs caddy --tail 30     # debe mostrar "certificate obtained successfully"
docker compose logs app --tail 40
curl -I https://TU_DOMINIO/
```
La primera build en ARM tarda unos minutos. Si Caddy no obtiene el certificado, casi
siempre es DNS que no apunta a la IP o puertos 80/443 sin abrir (paso 2).

## 7. Migrar datos desde XAMPP (opcional)
En tu PC:
```bash
"/c/xampp/mysql/bin/mysqldump.exe" -uroot --databases escuela-empresa --single-transaction --routines --triggers > dump.sql
scp -i ~/.ssh/tu_clave dump.sql ubuntu@IP_PUBLICA:/tmp/dump.sql
```
En la VM:
```bash
cd ~/Escuela-Empresa
docker compose stop app
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < /tmp/dump.sql
docker compose start app
```
Los uploads viejos (carpeta `uploads/planillas`) se copian al volumen con
`docker cp uploads/. $(docker compose ps -q app):/app/uploads/` y luego
`docker compose exec -u root app chown -R appuser:appuser /app/uploads`.

## 8. App Android
Cambiar la URL base del servidor a `https://<tu-dominio>` y regenerar la APK.

## Operación diaria
```bash
cd ~/Escuela-Empresa && git pull && docker compose up -d --build   # actualizar
docker compose logs -f app                                          # logs en vivo
docker compose restart                                              # reiniciar
```
**Backup de la base** (hacelo periódico, Oracle puede reclamar VMs inactivas):
```bash
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases escuela-empresa' > backup-$(date +%F).sql
```
No uses `docker compose down -v`: borra la base y los uploads.
