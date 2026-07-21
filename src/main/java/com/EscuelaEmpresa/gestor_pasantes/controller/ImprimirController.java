package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ImprimirController {
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;

    public ImprimirController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    // @GetMapping("/imprimir")
    // public String imprimir() {
    //     return "alumno/imprimir";
    // }

    @GetMapping("/imprimir")
    public void imprimirPdf(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Buscar los datos del alumno logueado
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Alumno alumno = alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        // 2. Cargar la plantilla PDF
        File archivoOriginal = new File("src/main/resources/plantillas/CONTRATO_PEL_2025.pdf");
        PDDocument document = Loader.loadPDF(archivoOriginal);
        PDPage pagina = document.getPage(0);

        // 3. Abrir el "lienzo" para escribir encima del PDF
        PDPageContentStream contentStream = new PDPageContentStream(
                document, pagina, PDPageContentStream.AppendMode.APPEND, true, true);

        PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        float tamanioFuente = 10;

        // 4. Escribir cada dato en su posición (coordenadas ya calculadas con la grilla)
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getNombres() + " " + alumno.getApellidos(), 411, 860.3f);
        escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEspecialidad().getNombre(), 393, 847.1f);
        if (alumno.getEmpresa() != null) { //Si ya tiene una empresa asignada entonces si se coloca en el pdf, sino no
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, alumno.getEmpresa().getNombre(), 203, 821);
        }
        if (alumno.getPadreTutor() != null) { //Si ya tiene un Padre/Tutor entonces si se coloca en el pdf, sino no
            String nombrePadreTutor = alumno.getPadreTutor().getNombres() + " " + alumno.getPadreTutor().getApellidos();
            escribirTextoCentrado(contentStream, fuente, tamanioFuente, nombrePadreTutor, 437, 272);
        }

        // 5. Cerrar el lienzo (ya no se puede seguir escribiendo después de esto)
        contentStream.close();

        // 6. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=ficha_alumno.pdf");

        // 7. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    // Método reutilizable para centrar texto en una coordenada X específica
    private void escribirTextoCentrado(PDPageContentStream contentStream, PDType1Font fuente,
                                        float tamanioFuente, String texto,
                                        float centroX, float y) throws IOException {

        float anchoTexto = fuente.getStringWidth(texto) / 1000 * tamanioFuente;
        float posicionX = centroX - (anchoTexto / 2);

        contentStream.beginText();
        contentStream.setFont(fuente, tamanioFuente);
        contentStream.newLineAtOffset(posicionX, y);
        contentStream.showText(texto);
        contentStream.endText();
    }
}
