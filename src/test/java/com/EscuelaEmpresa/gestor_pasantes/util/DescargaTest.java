package com.EscuelaEmpresa.gestor_pasantes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;

class DescargaTest {

    // Se vuelve a parsear con la misma clase de Spring: lo que importa es que el
    // nombre sobreviva el viaje intacto, no la forma exacta del header.
    private static String nombreDe(String header) {
        return ContentDisposition.parse(header).getFilename();
    }

    @Test
    void acentosYEspaciosLleganEnteros() {
        String header = Descarga.adjunto("Informe_Pasantia_María Pérez.docx", "x.docx");
        assertTrue(header.startsWith("attachment;"));
        assertEquals("Informe_Pasantia_María Pérez.docx", nombreDe(header));
    }

    @Test
    void comillasYPuntoYComaNoRompenElHeader() {
        String header = Descarga.inline("mi\"; evil=1; x.pdf", "x.pdf");
        assertEquals("mi\"; evil=1; x.pdf", nombreDe(header));
        // La comilla va escapada dentro del valor entre comillas, y en filename*
        // (la forma que usan los navegadores modernos) todo va percent-encoded:
        // no queda un parametro "evil" suelto.
        assertTrue(header.contains("filename=\"mi\\\"; evil=1; x.pdf\""));
        assertTrue(header.contains("filename*=UTF-8''mi%22%3B%20evil%3D1%3B%20x.pdf"));
    }

    @Test
    void nombreNuloOVacioUsaElRespaldo() {
        assertEquals("documento_7.pdf", nombreDe(Descarga.inline(null, "documento_7.pdf")));
        assertEquals("documento_7.pdf", nombreDe(Descarga.inline("   ", "documento_7.pdf")));
    }

    @Test
    void unaRutaCompletaSeReduceAlNombre() {
        assertEquals("contrato.pdf", nombreDe(Descarga.inline("C:\\Users\\yo\\Desktop\\contrato.pdf", "x.pdf")));
        assertEquals("contrato.pdf", nombreDe(Descarga.inline("/home/yo/contrato.pdf", "x.pdf")));
    }
}
