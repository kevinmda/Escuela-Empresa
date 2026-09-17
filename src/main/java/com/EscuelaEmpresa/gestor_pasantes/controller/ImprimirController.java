package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.ExpedientePdfService;
import com.EscuelaEmpresa.gestor_pasantes.service.FormularioPdfService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.service.PlantillaService;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ImprimirController {
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlantillaService plantillaService;
    private final ExpedientePdfService expedientePdfService;
    private final LimitesDocumentoService limitesDocumentoService;
    private final FormularioPdfService formularioPdfService;

    public ImprimirController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                              PlanillaSemanalRepository planillaSemanalRepository, PlantillaService plantillaService,
                              ExpedientePdfService expedientePdfService, LimitesDocumentoService limitesDocumentoService,
                              FormularioPdfService formularioPdfService) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.plantillaService = plantillaService;
        this.expedientePdfService = expedientePdfService;
        this.limitesDocumentoService = limitesDocumentoService;
        this.formularioPdfService = formularioPdfService;
    }

    // Antes vivían las tres juntas en una sola vista (/alumno/imprimir); ahora
    // el desplegable "Documentos" del riel las separa en tres páginas. El
    // contenido y el CSS de cada tarjeta no cambian, solo se reparten.
    @GetMapping("/alumno/documentos/antes-de-empezar")
    public String mostrarDocumentosAntesDeEmpezar(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // Los tres bloqueados del formulario del Contrato: ya están en la base,
        // así que el alumno los ve pero no los toca.
        model.addAttribute("nombreCompletoAlumno", alumno.getNombres() + " " + alumno.getApellidos());
        model.addAttribute("especialidadAlumno",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getNombre() : "");

        Locale localeEspanol = new Locale.Builder().setLanguage("es").setRegion("ES").build();
        DateTimeFormatter formatoLargo = new DateTimeFormatterBuilder()
                .appendPattern("d 'de' MMMM 'de' yyyy")
                .toFormatter(localeEspanol);
        model.addAttribute("fechaHoy", LocalDate.now().format(formatoLargo));

        return "alumno/documentos-antes-de-empezar";
    }

    @GetMapping("/alumno/documentos/al-terminar")
    public String mostrarDocumentosAlTerminar() {
        return "alumno/documentos-al-terminar";
    }

    @GetMapping("/alumno/documentos/adjuntos")
    public String mostrarDocumentosAdjuntos(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        model.addAttribute("expedienteCompleto", expedientePdfService.estaCompleto(alumno.getIdAl()));
        model.addAttribute("totalSubidos", limitesDocumentoService.contarTotalSubidos(alumno.getIdAl()));
        model.addAttribute("totalLimite", limitesDocumentoService.obtenerLimiteTotal());
        return "alumno/documentos-adjuntos";
    }

    @GetMapping("/alumno/imprimir/Documentos_Adjuntos.pdf")
    public void generarPdfDocumentosAdjuntos(Authentication authentication, HttpServletResponse response) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // Se arma primero en memoria: si el expediente todavía no está completo,
        // generarPdf tira una excepción antes de que toquemos la respuesta, y
        // ManejoErroresGlobal puede mostrar la página de error en vez de un PDF a medio escribir.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        expedientePdfService.generarPdf(alumno, buffer);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", Descarga.inline("Documentos_Adjuntos.pdf", "Documentos_Adjuntos.pdf"));
        buffer.writeTo(response.getOutputStream());
    }

    // supervisor y empresa son obligatorios en el formulario (ver
    // documentos-antes-de-empezar.html); area es el único opcional. Sin
    // required=false, Spring devuelve 400 si faltan -- ya cubierto por el
    // required del HTML y por el botón deshabilitado hasta llenarlos.
    @GetMapping("/alumno/imprimir/Contrato.pdf")
    public void generarPdfContrato(@RequestParam String supervisor,
                                    @RequestParam String empresa,
                                    @RequestParam(required = false) String area,
                                    Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        byte[] pdf = formularioPdfService.generarContrato(alumno, supervisor.trim(), empresa.trim(),
                area != null ? area.trim() : null);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/alumno/imprimir/Autorizacion.pdf")
    public void generarPdfAutorizacion(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        byte[] pdf = formularioPdfService.generarAutorizacion(alumno);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Autorizacion_alumno.pdf");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Pel.pdf")
    public void generarPdfFichaFinalPel(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        List<PlanillaSemanal> todasLasPlanillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        byte[] pdf = formularioPdfService.generarFichaFinal(alumno, todasLasPlanillas);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Ficha_Final_alumno.pdf");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Eval_Pel.pdf")
    public void generarPdfFichaFinalEvalPel(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        byte[] pdf = formularioPdfService.generarFichaFinalEvaluativa(alumno);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Ficha_Final_Eval_alumno.pdf");
        response.getOutputStream().write(pdf);
    }

    //----------------------------------------------------------------//
    @GetMapping("/alumno/imprimir/Contrato_Vacio.pdf")
    public void generarPdfContratoVacio(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("CONTRATO_PEL_2025.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Autorizacion_Vacio.pdf")
    public void generarPdfAutorizacionVacio(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("AUTORIZACION_PADRES_PEL_25.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Pel_Vacio.pdf")
    public void generarPdfFichaFinalPelVacio(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_PEL_2025.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Eval_Pel_Vacio.pdf")
    public void generarPdfFichaFinalEvalPelVacio(Authentication authentication, HttpServletResponse response) throws IOException {

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_EVAL_PASANTE_PEL_2025.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Contrato_alumno.pdf");

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }
    //----------------------------------------------------------------//

    private Alumno obtenerAlumnoAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return alumnoRepository.findByUsuario_IdUsr(usuario.getIdUsr())
            .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }
}
