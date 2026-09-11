package com.EscuelaEmpresa.gestor_pasantes.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ValidacionDocumentoService {

    /**
     * Valida que el archivo sea un PDF válido verificando:
     * 1. Que el content-type sea application/pdf
     * 2. Que los magic numbers (PDF signature) sean correctos
     * 3. Que no esté corrupto
     */
    public boolean validarPdfIntegridad(MultipartFile archivo) throws IOException {
        // Verificar content-type
        if (!("application/pdf".equals(archivo.getContentType()) 
              || (archivo.getOriginalFilename() != null 
                  && archivo.getOriginalFilename().toLowerCase().endsWith(".pdf")))) {
            return false;
        }

        // Verificar magic numbers (PDF siempre comienza con "%PDF")
        byte[] header = new byte[4];
        try (var inputStream = archivo.getInputStream()) {
            if (inputStream.read(header) < 4) {
                return false;
            }
            
            // Verificar que comience con "%PDF"
            if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                return false; // No es un PDF válido
            }
        }

        return true;
    }

    /**
     * Calcula el hash SHA-256 de un archivo usando su Path
     */
    public String calcularSHA256(Path rutaArchivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (var fis = Files.newInputStream(rutaArchivo)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no está disponible", e);
        }
    }

    /**
     * Calcula el hash SHA-256 de un MultipartFile
     */
    public String calcularSHA256(MultipartFile archivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (var inputStream = archivo.getInputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no está disponible", e);
        }
    }

    /**
     * Realiza una validación completa del documento:
     * 1. Verifica que sea un PDF válido
     * 2. Calcula su hash SHA-256
     * 3. Verifica tamaño máximo
     */
    public ValidacionResultado validarDocumentoCompleto(MultipartFile archivo, long tamanioMaximo) 
            throws IOException {
        
        ValidacionResultado resultado = new ValidacionResultado();

        // Verificar tamaño
        if (archivo.getSize() > tamanioMaximo) {
            resultado.addError("El archivo excede el tamaño máximo permitido (" + 
                             (tamanioMaximo / (1024 * 1024)) + " MB)");
            return resultado;
        }

        // Validar que sea PDF
        if (!validarPdfIntegridad(archivo)) {
            resultado.addError("El archivo no es un PDF válido");
            return resultado;
        }

        // Calcular hash
        try {
            String hash = calcularSHA256(archivo);
            resultado.setHashCalculado(hash);
            resultado.setValido(true);
        } catch (IOException e) {
            resultado.addError("Error al procesar el archivo: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Clase para retornar los resultados de la validación
     */
    public static class ValidacionResultado {
        private boolean valido = false;
        private String hashCalculado;
        private StringBuilder errores = new StringBuilder();

        public ValidacionResultado() {
        }

        public boolean isValido() {
            return valido;
        }

        public void setValido(boolean valido) {
            this.valido = valido;
        }

        public String getHashCalculado() {
            return hashCalculado;
        }

        public void setHashCalculado(String hashCalculado) {
            this.hashCalculado = hashCalculado;
        }

        public String getErrores() {
            return errores.toString();
        }

        public void addError(String error) {
            if (errores.length() > 0) {
                errores.append("; ");
            }
            errores.append(error);
        }

        public boolean tieneErrores() {
            return errores.length() > 0;
        }
    }
}
