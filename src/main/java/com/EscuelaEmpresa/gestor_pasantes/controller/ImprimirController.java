package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PadreTutor;
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

    @GetMapping("/alumno/imprimir/Documentos_Adjuntos.pdf")
    public void generarPdfDocumentosAdjuntos(Authentication authentication, HttpServletResponse response) throws IOException {

        Alumno alumno = obtenerAlumnoAutenticado(authentication);

        // Se arma primero en memoria: si el expediente todavía no está completo,
        // generarPdf tira una excepción antes de que toquemos la respuesta, y
        // ManejoErroresGlobal puede mostrar la página de error en vez de un PDF a medio escribir.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        expedientePdfService.generarPdf(alumno, buffer);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Documentos_Adjuntos", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Documentos_Adjuntos.pdf"));
        buffer.writeTo(response.getOutputStream());
    }

    // Chequeo de solo lectura (GET, sin efectos secundarios) que el JS de la
    // página llama por fetch ANTES de someter el formulario de verdad. Sirve
    // para el Padre/Encargado del Contrato (solo nombre) y para el de la
    // Autorización (nombre + cédula) -- por eso "ci" es opcional. El objetivo es
    // que un dato que no va a coincidir nunca abra la pestaña nueva del PDF: sin
    // este paso previo, el navegador ya abrió la pestaña para cuando el servidor
    // se entera de que hay que rechazarlo.
    @GetMapping("/alumno/documentos/verificar-padre")
    @ResponseBody
    public Map<String, Object> verificarPadre(@RequestParam String nombre,
                                               @RequestParam(required = false) String ci,
                                               Authentication authentication) {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PadreTutor padreTutor = alumno.getPadreTutor();

        if (padreTutor == null) {
            return Map.of("valido", false, "error",
                    "Todavía no hay un padre, madre o tutor registrado para vos. Consultá con la coordinación.");
        }

        boolean nombreOk = nombreCoincide(nombre, padreTutor);
        boolean ciOk = ci == null || ci.isBlank() || ciCoincide(ci, padreTutor);

        if (!nombreOk || !ciOk) {
            String mensaje = (ci == null || ci.isBlank())
                    ? "El nombre y apellido del Padre/Encargado no coincide con lo que tenemos registrado. "
                            + "Revisalo e intentá de nuevo."
                    : "El nombre y apellido o la cédula del padre, madre o tutor no coinciden con lo que "
                            + "tenemos registrado. Revisalos e intentá de nuevo.";
            return Map.of("valido", false, "error", mensaje);
        }

        return Map.of("valido", true);
    }

    // supervisor, area y padreEncargado vienen del formulario (ver
    // documentos-antes-de-empezar.html); area es el único opcional. empresa NO
    // se recibe del formulario -- ese campo está bloqueado ahí porque es de la
    // base, así que se resuelve acá con el mismo criterio que el modelo de la
    // página, no con lo que mande el cliente en ese campo de solo lectura.
    //
    // padreEncargado se verifica igual que en Autorización (ver
    // generarPdfAutorizacion): esto es la validación del lado del servidor, la
    // misma que ya corre /alumno/documentos/verificar-padre antes de que el
    // navegador llegue a someter este formulario. No hay que confiar solo en el
    // chequeo del cliente -- alguien podría saltearse el JS.
    @GetMapping("/alumno/imprimir/Contrato.pdf")
    public String generarPdfContrato(@RequestParam String supervisor,
                                    @RequestParam(required = false) String area,
                                    @RequestParam String padreEncargado,
                                    Authentication authentication, HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PadreTutor padreTutor = alumno.getPadreTutor();

        if (padreTutor == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no hay un padre, madre o tutor registrado para vos. Consultá con la coordinación.");
            return "redirect:/alumno/documentos/antes-de-empezar";
        }

        if (!nombreCoincide(padreEncargado, padreTutor)) {
            redirectAttributes.addFlashAttribute("error",
                    "El nombre y apellido del Padre/Encargado no coincide con lo que tenemos registrado. "
                            + "Revisalo e intentá de nuevo.");
            return "redirect:/alumno/documentos/antes-de-empezar";
        }

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
    // dos campos que el alumno tiene que llenar Y que tienen que coincidir con
    // el Padre_Tutor que ya está cargado en la base (ver nombreCoincide/
    // ciCoincide), más dos adjuntos opcionales que se agregan como páginas
    // nuevas al final del PDF. Un archivo implica multipart/form-data, que un
    // <form method="get"> no puede mandar (el navegador solo manda el nombre
    // del archivo, no su contenido), así que pasa a POST.
    @PostMapping("/alumno/imprimir/Autorizacion.pdf")
    public String generarPdfAutorizacion(@RequestParam String padreNombre,
                                          @RequestParam String padreCi,
                                          @RequestParam(required = false) MultipartFile cedulaAlumno,
                                          @RequestParam(required = false) MultipartFile cedulaPadre,
                                          Authentication authentication, HttpServletResponse response,
                                          RedirectAttributes redirectAttributes) throws IOException {
        Alumno alumno = obtenerAlumnoAutenticado(authentication);
        PadreTutor padreTutor = alumno.getPadreTutor();

        if (padreTutor == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Todavía no hay un padre, madre o tutor registrado para vos. Consultá con la coordinación.");
            return "redirect:/alumno/documentos/antes-de-empezar";
        }

        if (!nombreCoincide(padreNombre, padreTutor) || !ciCoincide(padreCi, padreTutor)) {
            redirectAttributes.addFlashAttribute("error",
                    "El nombre y apellido o la cédula del padre, madre o tutor no coinciden con lo que "
                            + "tenemos registrado. Revisalos e intentá de nuevo.");
            return "redirect:/alumno/documentos/antes-de-empezar";
        }

        List<MultipartFile> adjuntos = new ArrayList<>();
        for (MultipartFile adjunto : Arrays.asList(cedulaAlumno, cedulaPadre)) {
            if (adjunto == null || adjunto.isEmpty()) {
                continue;
            }
            if (adjunto.getSize() > tamanioMaximo.toBytes()) {
                redirectAttributes.addFlashAttribute("error",
                        "Una de las cédulas adjuntas supera el tamaño máximo permitido ("
                                + tamanioMaximo.toMegabytes() + " MB).");
                return "redirect:/alumno/documentos/antes-de-empezar";
            }
            if (!validacionDocumentoService.validarPdfIntegridad(adjunto)) {
                redirectAttributes.addFlashAttribute("error", "Una de las cédulas adjuntas no es un PDF válido.");
                return "redirect:/alumno/documentos/antes-de-empezar";
            }
            adjuntos.add(adjunto);
        }

        byte[] pdf = formularioPdfService.generarAutorizacion(alumno);
        pdf = agregarPaginasAdjuntas(pdf, adjuntos);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(Descarga.nombreDocumento("Autorizacion", alumno.getNombres(), alumno.getApellidos(), "pdf"),
                        "Autorizacion.pdf"));
        response.getOutputStream().write(pdf);
        return null;
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

        // 1. Cargar la plantilla PDF -- la misma que usa la versión con datos
        // (AUTORIZACION_PADRES_PEL.pdf); antes esta apuntaba a
        // AUTORIZACION_PADRES_PEL_25.pdf, una versión vieja que ya no es la que
        // está en uso (esa incluso todavía traía el "202…" del año, que se sacó).
        PDDocument document = plantillaService.cargarPdf("AUTORIZACION_PADRES_PEL.pdf");

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

    // Compara por conjunto de palabras y no como texto exacto: "Juan Pérez" y
    // "Pérez Juan" son la misma persona, y un doble espacio o una mayúscula de
    // más no deberían bloquear a nadie. Sí hace falta que sean, palabra por
    // palabra, las mismas -- omitir un nombre o escribir uno de más no coincide.
    private boolean nombreCoincide(String nombreIngresado, PadreTutor padreTutor) {
        Set<String> ingresado = palabrasNormalizadas(nombreIngresado);
        Set<String> base = palabrasNormalizadas(padreTutor.getNombres() + " " + padreTutor.getApellidos());
        return !ingresado.isEmpty() && ingresado.equals(base);
    }

    // Se sacan puntos y espacios antes de comparar: "1.234.567" y "1234567" son
    // la misma cédula escrita distinto, no deberían contar como que no coincide.
    private boolean ciCoincide(String ciIngresado, PadreTutor padreTutor) {
        String limpio1 = ciIngresado == null ? "" : ciIngresado.replaceAll("[^0-9A-Za-z]", "");
        String limpio2 = padreTutor.getCi() == null ? "" : padreTutor.getCi().replaceAll("[^0-9A-Za-z]", "");
        return !limpio1.isEmpty() && limpio1.equalsIgnoreCase(limpio2);
    }

    private Set<String> palabrasNormalizadas(String texto) {
        if (texto == null) {
            return Set.of();
        }
        // NFD + sacar los caracteres combinantes (\p{M}) es como se sacan los
        // acentos sin mapear letra por letra: "María" y "Maria" quedan iguales.
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        if (normalizado.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalizado.split("\\s+")));
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
