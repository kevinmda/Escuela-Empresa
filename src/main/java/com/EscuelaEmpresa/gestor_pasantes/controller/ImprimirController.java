package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
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
import com.EscuelaEmpresa.gestor_pasantes.service.ValidacionDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class ImprimirController {

    // Claves de sesión donde generarPdfAutorizacion deja el PDF listo para que
    // descargarAutorizacionLista lo sirva una sola vez (ver esos dos métodos).
    private static final String SESION_AUTORIZACION_PDF = "autorizacionPdfBytes";
    private static final String SESION_AUTORIZACION_NOMBRE = "autorizacionPdfNombre";
    // Lo mismo para el expediente completo con el informe agregado al final.
    private static final String SESION_ADJUNTOS_PDF = "adjuntosPdfBytes";
    private static final String SESION_ADJUNTOS_NOMBRE = "adjuntosPdfNombre";

    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final PlantillaService plantillaService;
    private final ExpedientePdfService expedientePdfService;
    private final LimitesDocumentoService limitesDocumentoService;
    private final FormularioPdfService formularioPdfService;
    private final ValidacionDocumentoService validacionDocumentoService;

    // Mismo tope y misma fuente que ya usa SubirController: el valor real lo
    // aplica Spring antes de que esto se ejecute, así que conviene usar el
    // mismo número para el mensaje de error en vez de inventar uno aparte.
    private final DataSize tamanioMaximo;

    public ImprimirController(UsuarioRepository usuarioRepository, AlumnoRepository alumnoRepository,
                              PlanillaSemanalRepository planillaSemanalRepository, PlantillaService plantillaService,
                              ExpedientePdfService expedientePdfService, LimitesDocumentoService limitesDocumentoService,
                              FormularioPdfService formularioPdfService,
                              ValidacionDocumentoService validacionDocumentoService,
                              @Value("${spring.servlet.multipart.max-file-size}") DataSize tamanioMaximo) {
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.plantillaService = plantillaService;
        this.expedientePdfService = expedientePdfService;
        this.limitesDocumentoService = limitesDocumentoService;
        this.formularioPdfService = formularioPdfService;
        this.validacionDocumentoService = validacionDocumentoService;
        this.tamanioMaximo = tamanioMaximo;
    }

    // Antes vivían las tres juntas en una sola vista (/alumno/imprimir); ahora
    // el desplegable "Documentos" del riel las separa en tres páginas. El
    // contenido y el CSS de cada tarjeta no cambian, solo se reparten.
    @GetMapping("/alumno/documentos/antes-de-empezar")
    public String mostrarDocumentosAntesDeEmpezar(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // Bloqueados en los dos formularios de esta página (Contrato y
        // Autorización): ya están en la base, así que el alumno los ve pero no
        // los toca.
        model.addAttribute("nombreCompletoAlumno", alumno.getNombres() + " " + alumno.getApellidos());
        model.addAttribute("ciAlumno", alumno.getCi());
        model.addAttribute("especialidadAlumno",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getNombre() : "");
        model.addAttribute("empresaAlumno",
                alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : "Todavía sin asignar");

        return "alumno/documentos-antes-de-empezar";
    }

    @GetMapping("/alumno/documentos/al-terminar")
    public String mostrarDocumentosAlTerminar(Model model, Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        boolean tieneSeisPlanillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl()).size() >= 6;
        model.addAttribute("tieneSeisPlanillas", tieneSeisPlanillas);
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

    // Expediente completo + informe final. El informe NO se guarda en la base ni
    // en el disco: el alumno lo elige en la tarjeta de Documentos adjuntos, se
    // agrega al final del expediente y listo. Mismo esquema que Autorización
    // (POST con archivo por fetch -> JSON con la URL -> GET común que abre el
    // PDF): ver generarPdfAutorizacion y descargarAutorizacionLista.
    @PostMapping("/alumno/imprimir/Documentos_Adjuntos.pdf")
    @ResponseBody
    public Map<String, Object> generarPdfDocumentosAdjuntos(@RequestParam(required = false) MultipartFile informe,
                                                              Authentication authentication,
                                                              HttpServletRequest request) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        if (!expedientePdfService.estaCompleto(alumno.getIdAl())) {
            return Map.of("valido", false, "error",
                    "Todavía no subiste los 10 comprobantes del expediente en Subir documentos.");
        }
        if (informe == null || informe.isEmpty()) {
            return Map.of("valido", false, "error", "Elegí tu informe final en PDF para generar el expediente.");
        }
        if (informe.getSize() > tamanioMaximo.toBytes()) {
            return Map.of("valido", false, "error",
                    "El informe supera el tamaño máximo permitido (" + tamanioMaximo.toMegabytes() + " MB).");
        }
        if (!validacionDocumentoService.validarPdfIntegridad(informe)) {
            return Map.of("valido", false, "error", "El informe no es un PDF válido.");
        }

        // En memoria: si algo falla, no queda nada a medio escribir en la respuesta.
        ByteArrayOutputStream expediente = new ByteArrayOutputStream();
        expedientePdfService.generarPdf(alumno, expediente);
        byte[] pdf = agregarPaginasAdjuntas(expediente.toByteArray(), List.of(informe));

        HttpSession session = request.getSession();
        session.setAttribute(SESION_ADJUNTOS_PDF, pdf);
        session.setAttribute(SESION_ADJUNTOS_NOMBRE,
                Descarga.nombreDocumento("Documentos_Adjuntos", alumno.getNombres(), alumno.getApellidos(), "pdf"));

        return Map.of("valido", true, "url", "/alumno/imprimir/Documentos_Adjuntos-lista.pdf");
    }

    // El PDF que dejó listo generarPdfDocumentosAdjuntos en la sesión. Igual que
    // Autorizacion-lista.pdf, no se borra al servirlo (el visor de PDF del
    // navegador vuelve a pedir la misma URL para guardarlo).
    @GetMapping("/alumno/imprimir/Documentos_Adjuntos-lista.pdf")
    public void descargarDocumentosAdjuntosListo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        byte[] pdf = session != null ? (byte[]) session.getAttribute(SESION_ADJUNTOS_PDF) : null;

        if (pdf == null) {
            throw new ReglaNegocioException("No hay ningún expediente listo para descargar. "
                    + "Elegí tu informe y generalo de nuevo.");
        }

        String nombreArchivo = (String) session.getAttribute(SESION_ADJUNTOS_NOMBRE);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", Descarga.inline(nombreArchivo, "Documentos_Adjuntos.pdf"));
        response.getOutputStream().write(pdf);
    }

    // supervisor, area y padreEncargado vienen del formulario (ver
    // documentos-antes-de-empezar.html); area es el único opcional. empresa NO
    // se recibe del formulario -- ese campo está bloqueado ahí porque es de la
    // base, así que se resuelve acá con el mismo criterio que el modelo de la
    // página, no con lo que mande el cliente en ese campo de solo lectura.
    //
    // padreEncargado se escribe en el PDF tal cual lo ingresó el alumno: no se
    // compara con el Padre_Tutor cargado en la base.
    @GetMapping("/alumno/imprimir/Contrato.pdf")
    public String generarPdfContrato(@RequestParam String supervisor,
                                    @RequestParam(required = false) String area,
                                    @RequestParam String padreEncargado,
                                    Authentication authentication, HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        String empresa = alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : null;

        byte[] pdf = formularioPdfService.generarContrato(alumno, supervisor.trim(), empresa,
                area != null ? area.trim() : null, padreEncargado.trim());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Contrato", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Contrato.pdf"));
        response.getOutputStream().write(pdf);
        return null;
    }

    // Antes era un link fijo (GET, sin parámetros); ahora es un formulario con
    // dos campos que el alumno tiene que llenar (se escriben en el PDF tal cual,
    // sin compararlos con el Padre_Tutor de la base), más dos adjuntos opcionales que se agregan como páginas
    // nuevas al final del PDF. Un archivo implica multipart/form-data, que un
    // <form method="get"> no puede mandar (el navegador solo manda el nombre
    // del archivo, no su contenido), así que pasa a POST.
    // Se llama por fetch, así que responde JSON en
    // vez de escribir el PDF directo o redirigir. La razón de fondo es la
    // misma por la que este formulario pasó a POST: el archivo. Pero un POST
    // con archivos, abierto en una pestaña nueva (target="_blank"), es
    // exactamente la combinación que algunos navegadores reportan como
    // "problema de red" -- y una vez resuelto eso con fetch, un blob abierto
    // con window.open() no respeta el nombre del archivo (el navegador
    // termina mostrando el identificador interno del blob). Por eso esto NO
    // devuelve el PDF: lo genera, lo deja guardado en la sesión, y devuelve
    // nada más que la URL de descargarAutorizacionLista de abajo -- esa sí es
    // una navegación GET normal y corriente, igual que la de cualquier otro
    // documento de esta página, así que el navegador la abre en una pestaña
    // con el nombre correcto sin ningún truco de por medio.
    @PostMapping("/alumno/imprimir/Autorizacion.pdf")
    @ResponseBody
    public Map<String, Object> generarPdfAutorizacion(@RequestParam String padreNombre,
                                                        @RequestParam String padreCi,
                                                        @RequestParam(required = false) MultipartFile cedulaAlumno,
                                                        @RequestParam(required = false) MultipartFile cedulaPadre,
                                                        Authentication authentication,
                                                        HttpServletRequest request) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        List<MultipartFile> adjuntos = new ArrayList<>();
        for (MultipartFile adjunto : Arrays.asList(cedulaAlumno, cedulaPadre)) {
            if (adjunto == null || adjunto.isEmpty()) {
                continue;
            }
            if (adjunto.getSize() > tamanioMaximo.toBytes()) {
                return Map.of("valido", false, "error",
                        "Una de las cédulas adjuntas supera el tamaño máximo permitido ("
                                + tamanioMaximo.toMegabytes() + " MB).");
            }
            if (!validacionDocumentoService.validarPdfIntegridad(adjunto)) {
                return Map.of("valido", false, "error", "Una de las cédulas adjuntas no es un PDF válido.");
            }
            adjuntos.add(adjunto);
        }

        byte[] pdf = formularioPdfService.generarAutorizacion(alumno, padreNombre.trim(), padreCi.trim());
        pdf = agregarPaginasAdjuntas(pdf, adjuntos);

        HttpSession session = request.getSession();
        session.setAttribute(SESION_AUTORIZACION_PDF, pdf);
        session.setAttribute(SESION_AUTORIZACION_NOMBRE,
                Descarga.nombreDocumento("Autorizacion", alumno.getNombres(), alumno.getApellidos(), "pdf"));

        return Map.of("valido", true, "url", "/alumno/imprimir/Autorizacion-lista.pdf");
    }

    // El PDF que generarPdfAutorizacion dejó listo en la sesión: esta es la
    // navegación GET común y corriente (sin archivos, sin POST) que el JS abre
    // con window.open() -- el mismo mecanismo que ya usan Contrato y Ficha
    // Final, así que el navegador la trata igual: la abre en una pestaña con
    // el nombre correcto.
    //
    // A propósito NO se borra de la sesión después de servirlo: el botón de
    // "Guardar"/"Descargar" que trae el visor de PDF del navegador no reusa
    // los bytes que ya mostró, vuelve a pedir esta misma URL para guardarla
    // -- si el primer pedido ya lo hubiera borrado, esa segunda vez
    // encontraba la sesión vacía y terminaba mostrando (y guardando) la
    // página de error genérica en vez del PDF. Se sobreescribe solo la
    // próxima vez que se genera una Autorización nueva.
    @GetMapping("/alumno/imprimir/Autorizacion-lista.pdf")
    public void descargarAutorizacionLista(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        byte[] pdf = session != null ? (byte[]) session.getAttribute(SESION_AUTORIZACION_PDF) : null;

        if (pdf == null) {
            throw new ReglaNegocioException("No hay ningún documento de Autorización listo para descargar. "
                    + "Volvé a completar el formulario y generalo de nuevo.");
        }

        String nombreArchivo = (String) session.getAttribute(SESION_AUTORIZACION_NOMBRE);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", Descarga.inline(nombreArchivo, "Autorizacion.pdf"));
        response.getOutputStream().write(pdf);
    }

    // area es obligatorio en el formulario (ver documentos-al-terminar.html),
    // que además deja el campo deshabilitado hasta que hay 6 planillas -- pero
    // eso es solo la puerta de entrada del lado del cliente. El chequeo real es
    // este: si alguien llega igual con menos de 6 (JS desactivado, URL armada a
    // mano), se rechaza acá antes de generar nada.
    @GetMapping("/alumno/imprimir/Ficha_Final_Pel.pdf")
    public String generarPdfFichaFinalPel(@RequestParam String area,
                                         Authentication authentication, HttpServletResponse response,
                                         RedirectAttributes redirectAttributes) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        List<PlanillaSemanal> todasLasPlanillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());

        if (todasLasPlanillas.size() < 6) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no completaste las 6 semanas de planilla, no se puede generar la ficha final.");
            return "redirect:/alumno/documentos/al-terminar";
        }

        byte[] pdf = formularioPdfService.generarFichaFinal(alumno, todasLasPlanillas, area.trim());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Ficha_Final", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Ficha_Final.pdf"));
        response.getOutputStream().write(pdf);
        return null;
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Eval_Pel.pdf")
    public void generarPdfFichaFinalEvalPel(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        byte[] pdf = formularioPdfService.generarFichaFinalEvaluativa(alumno);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Ficha_Final_Eval", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Ficha_Final_Eval.pdf"));
        response.getOutputStream().write(pdf);
    }

    //----------------------------------------------------------------//
    @GetMapping("/alumno/imprimir/Contrato_Vacio.pdf")
    public void generarPdfContratoVacio(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("CONTRATO_PEL_2026.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Contrato", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Contrato.pdf"));

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Autorizacion_Vacio.pdf")
    public void generarPdfAutorizacionVacio(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 1. Cargar la plantilla PDF -- distinta de la que usa la versión con
        // datos: esta trae "202…" para completar el año a mano, pensada para
        // llenarse con lapicera.
        PDDocument document = plantillaService.cargarPdf("AUTORIZACION_PADRES_PEL_25.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Autorizacion", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Autorizacion.pdf"));

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Pel_Vacio.pdf")
    public void generarPdfFichaFinalPelVacio(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_PEL.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Ficha_Final", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Ficha_Final.pdf"));

        // 3. Enviar el PDF final al navegador
        document.save(response.getOutputStream());
        document.close();
    }

    @GetMapping("/alumno/imprimir/Ficha_Final_Eval_Pel_Vacio.pdf")
    public void generarPdfFichaFinalEvalPelVacio(Authentication authentication, HttpServletResponse response) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // 1. Cargar la plantilla PDF
        PDDocument document = plantillaService.cargarPdf("FICHA_FINAL_EVAL_PASANTE_PEL.pdf");

        // 2. Configurar la respuesta HTTP para que el navegador muestre el PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Ficha_Final_Eval", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Ficha_Final_Eval.pdf"));

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

    // PDFMergerUtility.appendDocument y no un bucle con importPage: es la
    // herramienta que trae PDFBox específicamente para esto (agregar todas las
    // páginas de un documento al final de otro), y se hace cargo de recursos y
    // estructura interna de forma más completa que llamar importPage página por
    // página a mano.
    private byte[] agregarPaginasAdjuntas(byte[] pdfBase, List<MultipartFile> adjuntos) throws IOException {
        if (adjuntos.isEmpty()) {
            return pdfBase;
        }

        try (PDDocument documentoFinal = Loader.loadPDF(pdfBase)) {
            PDFMergerUtility fusionador = new PDFMergerUtility();

            for (MultipartFile adjunto : adjuntos) {
                try (PDDocument documentoAdjunto = Loader.loadPDF(adjunto.getBytes())) {
                    fusionador.appendDocument(documentoFinal, documentoAdjunto);
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documentoFinal.save(salida);
            return salida.toByteArray();
        }
    }
}
