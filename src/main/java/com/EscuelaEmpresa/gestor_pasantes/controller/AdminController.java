package com.EscuelaEmpresa.gestor_pasantes.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.EscuelaEmpresa.gestor_pasantes.exception.RecursoNoEncontradoException;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.dto.AlumnoFiltradoDTO;
import com.EscuelaEmpresa.gestor_pasantes.dto.DocumentoSubidoAdminDTO;
import com.EscuelaEmpresa.gestor_pasantes.entity.Administrador;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.Empresa;
import com.EscuelaEmpresa.gestor_pasantes.entity.Especialidad;
import com.EscuelaEmpresa.gestor_pasantes.entity.Supervisor;
import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.AdministradorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.AlumnoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.EmpresaRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.EspecialidadRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.SupervisorRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.dto.ResumenDocumentoTipoDTO;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitesDocumentoService;
import com.EscuelaEmpresa.gestor_pasantes.service.NombresDeArchivo;
import com.EscuelaEmpresa.gestor_pasantes.util.Descarga;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final AlumnoRepository alumnoRepository;
    private final SupervisorRepository supervisorRepository;
    private final EmpresaRepository empresaRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final LimitesDocumentoService limitesDocumentoService;

    public AdminController(UsuarioRepository usuarioRepository,
                            AdministradorRepository administradorRepository,
                            EspecialidadRepository especialidadRepository,
                            AlumnoRepository alumnoRepository,
                            SupervisorRepository supervisorRepository,
                            EmpresaRepository empresaRepository,
                            DocumentoSubidoRepository documentoSubidoRepository,
                            LimitesDocumentoService limitesDocumentoService) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
        this.especialidadRepository = especialidadRepository;
        this.alumnoRepository = alumnoRepository;
        this.supervisorRepository = supervisorRepository;
        this.empresaRepository = empresaRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.limitesDocumentoService = limitesDocumentoService;
    }

    @GetMapping("/admin/alumnos")
    public String alumnos(Model model, Authentication authentication) {
        cargarFiltroEnModelo(model, authentication);
        model.addAttribute("paginaActual", "alumnos");
        return "administrador/alumnos";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, Authentication authentication) {
        cargarFiltroEnModelo(model, authentication);
        model.addAttribute("paginaActual", "reportes");
        return "administrador/reportes";
    }

    // Arma los datos del filtro (Especialidad/es o especialidad fija) que usan tanto
    // la pantalla de Alumnos como la de Reportes
    private void cargarFiltroEnModelo(Model model, Authentication authentication) {
        Administrador admin = obtenerAdminAutenticado(authentication);
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        model.addAttribute("esAdministrativo", esAdministrativo);

        if (esAdministrativo) {
            model.addAttribute("especialidades", especialidadRepository.findAll());
        } else {
            model.addAttribute("especialidadFija", obtenerEspecialidadDeCoordinador(admin));
        }
    }

    // --- Endpoints AJAX para el filtro en cascada ---

    @GetMapping("/admin/api/cursos")
    @ResponseBody
    public List<String> cursosPorEspecialidad(@RequestParam Integer idEsp, Authentication authentication) {
        verificarEspecialidadPermitida(idEsp, authentication);
        return alumnoRepository.findCursosPorEspecialidad(idEsp);
    }

    @GetMapping("/admin/api/secciones")
    @ResponseBody
    public List<String> seccionesPorEspecialidadYCurso(@RequestParam Integer idEsp, @RequestParam String curso,
                                                       Authentication authentication) {
        verificarEspecialidadPermitida(idEsp, authentication);
        return alumnoRepository.findSeccionesPorEspecialidadYCurso(idEsp, curso);
    }

    @GetMapping("/admin/api/alumnos-filtrados")
    @ResponseBody
    public List<AlumnoFiltradoDTO> alumnosFiltrados(@RequestParam Integer idEsp,
                                                      @RequestParam String curso,
                                                      @RequestParam String seccion,
                                                      Authentication authentication) {
        verificarEspecialidadPermitida(idEsp, authentication);
        return alumnoRepository.findByEspecialidad_IdEspAndCursoAndSeccion(idEsp, curso, seccion)
                .stream()
                .map(AlumnoFiltradoDTO::new)
                .toList();
    }

    // --- Búsqueda por nombre, apellido o CI (pantallas Alumnos y Reportes) ---

    @GetMapping("/admin/api/buscar-alumnos")
    @ResponseBody
    public List<AlumnoFiltradoDTO> buscarAlumnos(@RequestParam String texto, Authentication authentication) {
        Administrador admin = obtenerAdminAutenticado(authentication);
        List<Integer> idsEspPermitidos = obtenerIdsEspecialidadPermitidos(admin);

        return alumnoRepository.buscarPorNombreApellidoOCi(idsEspPermitidos, texto)
                .stream()
                .map(AlumnoFiltradoDTO::new)
                .toList();
    }

    // Devuelve los ids de especialidad dentro de los que este admin puede buscar/operar:
    // todas, si es administrativo; solo la propia, si es coordinador
    private List<Integer> obtenerIdsEspecialidadPermitidos(Administrador admin) {
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        if (esAdministrativo) {
            return especialidadRepository.findAll()
                    .stream()
                    .map(Especialidad::getIdEsp)
                    .toList();
        }

        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);
        return List.of(especialidadFija.getIdEsp());
    }

    private void verificarEspecialidadPermitida(Integer idEsp, Authentication authentication) {
        Administrador admin = obtenerAdminAutenticado(authentication);
        if (!obtenerIdsEspecialidadPermitidos(admin).contains(idEsp)) {
            throw new AccessDeniedException("No podés consultar otra especialidad");
        }
    }

    @GetMapping("/admin/api/alumnos/{idAl}/documentos")
    @ResponseBody
    public List<DocumentoSubidoAdminDTO> documentosSubidosPorAlumno(@PathVariable Integer idAl,
                                                                     Authentication authentication) {
        verificarAccesoAlumno(idAl, authentication);

        return documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(idAl)
                .stream()
                .map(documento -> new DocumentoSubidoAdminDTO(
                        documento.getIdDs(),
                        documento.getNombreArchivo(),
                        documento.getFechaSubida(),
                        "/admin/alumnos/" + idAl + "/documentos/" + documento.getIdDs() + "/ver",
                        documento.getTipoDocumento(),
                        documento.getValidado()))
                .toList();
    }

    // Resumen de entregas por tipo de documento (contador x/límite + fecha exacta de
    // cada entrega), para el menú desplegable de la pantalla de Reportes
    @GetMapping("/admin/api/alumnos/{idAl}/resumen-documentos")
    @ResponseBody
    public List<ResumenDocumentoTipoDTO> resumenDocumentosPorAlumno(@PathVariable Integer idAl,
                                                                     Authentication authentication) {
        verificarAccesoAlumno(idAl, authentication);

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(idAl);

        return Arrays.stream(TipoDocumento.values())
                .map(tipo -> {
                    List<DocumentoSubidoAdminDTO> documentosDeEsteTipo = documentos.stream()
                            .filter(documento -> documento.getTipoDocumento() == tipo)
                            .map(documento -> new DocumentoSubidoAdminDTO(
                                    documento.getIdDs(),
                                    documento.getNombreArchivo(),
                                    documento.getFechaSubida(),
                                    "/admin/alumnos/" + idAl + "/documentos/" + documento.getIdDs() + "/ver",
                                    documento.getTipoDocumento(),
                                    documento.getValidado()))
                            .toList();

                    int limite = limitesDocumentoService.obtenerLimitePorTipo(tipo);
                    return new ResumenDocumentoTipoDTO(tipo, limite, documentosDeEsteTipo);
                })
                .toList();
    }

    @GetMapping("/admin/alumnos/{idAl}/documentos/{idDs}/ver")
    public void verDocumentoSubido(@PathVariable Integer idAl,
                                   @PathVariable Integer idDs,
                                   Authentication authentication,
                                   HttpServletResponse response) throws IOException {
        Alumno alumno = verificarAccesoAlumno(idAl, authentication);
        DocumentoSubido documento = documentoSubidoRepository.findById(idDs)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

        if (!documento.getAlumno().getIdAl().equals(alumno.getIdAl())) {
            throw new AccessDeniedException("Ese documento no pertenece al alumno indicado");
        }

        File archivo = new File(documento.getRutaArchivo());
        if (!archivo.exists()) {
            throw new RecursoNoEncontradoException("El archivo ya no está disponible en el servidor");
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                Descarga.inline(documento.getNombreArchivo(), "documento_" + documento.getIdDs() + ".pdf"));
        Files.copy(archivo.toPath(), response.getOutputStream());
    }

    // --- Descarga en ZIP de los documentos subidos por los alumnos filtrados ---

    @GetMapping("/admin/alumnos/zip")
    public void descargarZip(@RequestParam Integer idEsp,
                              @RequestParam String curso,
                              @RequestParam String seccion,
                              Authentication authentication,
                              HttpServletResponse response) throws IOException {

        verificarEspecialidadPermitida(idEsp, authentication);

        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEspAndCursoAndSeccion(idEsp, curso, seccion);

        String nombreZip = "Documentos_" + sanitizar(curso) + "_" + sanitizar(seccion) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreZip);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            for (Alumno alumno : alumnos) {

                String carpetaAlumno = sanitizar(alumno.getApellidos() + "_" + alumno.getNombres());

                // Documentos que el alumno subió directamente (contrato firmado, fotos, etc.)
                List<DocumentoSubido> documentosSubidos =
                        documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());

                for (DocumentoSubido documento : documentosSubidos) {

                    File archivoFisico = new File(documento.getRutaArchivo());
                    if (!archivoFisico.exists()) {
                        continue; // si el archivo ya no está en el disco, lo salteamos sin romper el ZIP entero
                    }

                    // Usamos el nombre original con el que el alumno lo subió, no el UUID interno,
                    // para que sea legible cuando el admin/coordinador lo abra
                    String nombreOriginal = documento.getNombreArchivo() != null
                            ? new File(documento.getNombreArchivo()).getName()
                            : ("documento_" + documento.getIdDs() + ".pdf");
                        nombreOriginal = sanitizarNombreArchivo(nombreOriginal, documento.getIdDs());
                    String nombreArchivo = carpetaAlumno + "/Documentos_Subidos/" + nombreOriginal;

                    zos.putNextEntry(new ZipEntry(nombreArchivo));
                    Files.copy(archivoFisico.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    // --- Descarga en ZIP de los documentos de UN alumno puntual ---

    @GetMapping("/admin/alumnos/{idAl}/documentos/zip")
    public void descargarZipDeAlumno(@PathVariable Integer idAl,
                                      Authentication authentication,
                                      HttpServletResponse response) throws IOException {

        Alumno alumno = verificarAccesoAlumno(idAl, authentication);

        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(idAl);

        String nombreZip = "Documentos_" + sanitizar(alumno.getApellidos() + "_" + alumno.getNombres()) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreZip);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (DocumentoSubido documento : documentos) {

                File archivoFisico = new File(documento.getRutaArchivo());
                if (!archivoFisico.exists()) {
                    continue; // si el archivo ya no está en el disco, lo salteamos sin romper el ZIP entero
                }

                String nombreOriginal = documento.getNombreArchivo() != null
                        ? new File(documento.getNombreArchivo()).getName()
                        : ("documento_" + documento.getIdDs() + ".pdf");
                nombreOriginal = sanitizarNombreArchivo(nombreOriginal, documento.getIdDs());

                zos.putNextEntry(new ZipEntry(nombreOriginal));
                Files.copy(archivoFisico.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    // --- Gestión de Supervisores (solo Coordinador) ---

    @GetMapping("/admin/supervisores")
    public String supervisores(Model model, Authentication authentication) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        List<Supervisor> supervisores = supervisorRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());
        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());

        model.addAttribute("especialidadFija", especialidadFija);
        model.addAttribute("supervisores", supervisores);
        model.addAttribute("alumnos", alumnos);
        model.addAttribute("paginaActual", "supervisores");

        return "coordinador/supervisores";
    }

    @PostMapping("/admin/supervisores/crear")
    public String crearSupervisor(@RequestParam String nombres,
                                   @RequestParam String apellidos,
                                   @RequestParam String email,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Supervisor supervisor = new Supervisor();
        supervisor.setNombres(nombres);
        supervisor.setApellidos(apellidos);
        supervisor.setEmail(email);
        supervisor.setEspecialidad(especialidadFija);

        supervisorRepository.save(supervisor);

        redirectAttributes.addFlashAttribute("exito", "Supervisor agregado correctamente.");
        return "redirect:/admin/supervisores";
    }

    @PostMapping("/admin/supervisores/asignar")
    public String asignarSupervisor(@RequestParam Integer idAl,
                                     @RequestParam(required = false) Integer idSup,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Alumno alumno = alumnoRepository.findById(idAl)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        // Seguridad: el coordinador solo puede tocar alumnos de su propia especialidad
        if (!alumno.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés modificar un alumno de otra especialidad");
        }

        if (idSup == null) {
            alumno.setSupervisor(null); // se puede desasignar eligiendo "-- Sin asignar --"
        } else {
            Supervisor supervisor = supervisorRepository.findById(idSup)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Supervisor no encontrado"));

            if (!supervisor.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
                throw new AccessDeniedException("Ese supervisor no pertenece a tu especialidad");
            }

            alumno.setSupervisor(supervisor);
        }

        alumnoRepository.save(alumno);

        redirectAttributes.addFlashAttribute("exito", "Supervisor actualizado correctamente.");
        return "redirect:/admin/supervisores";
    }

    @PostMapping("/admin/supervisores/editar")
    public String editarSupervisor(@RequestParam Integer idSup,
                                    @RequestParam String nombres,
                                    @RequestParam String apellidos,
                                    @RequestParam String email,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Supervisor supervisor = supervisorRepository.findById(idSup)
                .orElseThrow(() -> new RecursoNoEncontradoException("Supervisor no encontrado"));

        // Seguridad: el coordinador solo puede editar supervisores de su propia especialidad
        if (!supervisor.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés editar un supervisor de otra especialidad");
        }

        supervisor.setNombres(nombres);
        supervisor.setApellidos(apellidos);
        supervisor.setEmail(email);
        supervisorRepository.save(supervisor);

        redirectAttributes.addFlashAttribute("exito", "Supervisor editado correctamente.");
        return "redirect:/admin/supervisores";
    }

    @PostMapping("/admin/supervisores/eliminar")
    public String eliminarSupervisor(@RequestParam Integer idSup,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Supervisor supervisor = supervisorRepository.findById(idSup)
                .orElseThrow(() -> new RecursoNoEncontradoException("Supervisor no encontrado"));

        // Seguridad: el coordinador solo puede eliminar supervisores de su propia especialidad
        if (!supervisor.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés eliminar un supervisor de otra especialidad");
        }

        // Antes de borrar, hay que desasignarlo de cualquier alumno que lo tenga puesto,
        // porque la FK en la base no permite borrar un supervisor que todavía está en uso.
        // Un solo UPDATE: antes se traían todos los alumnos de la especialidad para
        // filtrarlos en memoria y guardarlos de a uno.
        alumnoRepository.desasignarSupervisor(idSup);

        supervisorRepository.delete(supervisor);

        redirectAttributes.addFlashAttribute("exito", "Supervisor eliminado correctamente.");
        return "redirect:/admin/supervisores";
    }

    private Especialidad obtenerEspecialidadDeCoordinador(Administrador admin) {
        boolean esAdministrativo = "administrativo".equalsIgnoreCase(admin.getCargo());

        if (esAdministrativo) {
            // La pantalla de Supervisores es exclusiva de Coordinación
            throw new AccessDeniedException("Esta pantalla es solo para coordinadores");
        }

        if (admin.getEspecialidad() == null) {
            throw new ReglaNegocioException("El coordinador no tiene especialidad asignada");
        }

        return admin.getEspecialidad();
    }

    // --- Gestión de Empresas (solo Coordinador) ---

    @GetMapping("/admin/empresas")
    public String empresas(Model model, Authentication authentication) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        List<Empresa> empresas = empresaRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());
        List<Alumno> alumnos = alumnoRepository.findByEspecialidad_IdEsp(especialidadFija.getIdEsp());

        model.addAttribute("especialidadFija", especialidadFija);
        model.addAttribute("empresas", empresas);
        model.addAttribute("alumnos", alumnos);
        model.addAttribute("paginaActual", "empresas");

        return "coordinador/empresas";
    }

    @PostMapping("/admin/empresas/crear")
    public String crearEmpresa(@RequestParam String nombre,
                                @RequestParam String ruc,
                                @RequestParam String telefono,
                                @RequestParam String email,
                                @RequestParam String direccion,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setRuc(ruc);
        empresa.setTelefono(telefono);
        empresa.setEmail(email);
        empresa.setDireccion(direccion);
        empresa.setEspecialidad(especialidadFija);

        empresaRepository.save(empresa);

        redirectAttributes.addFlashAttribute("exito", "Empresa agregada correctamente.");
        return "redirect:/admin/empresas";
    }

    @PostMapping("/admin/empresas/editar")
    public String editarEmpresa(@RequestParam Integer idEmp,
                                 @RequestParam String nombre,
                                 @RequestParam String ruc,
                                 @RequestParam String telefono,
                                 @RequestParam String email,
                                 @RequestParam String direccion,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Empresa empresa = empresaRepository.findById(idEmp)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));

        // Seguridad: el coordinador solo puede editar empresas de su propia especialidad
        if (empresa.getEspecialidad() == null || !empresa.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés editar una empresa de otra especialidad");
        }

        empresa.setNombre(nombre);
        empresa.setRuc(ruc);
        empresa.setTelefono(telefono);
        empresa.setEmail(email);
        empresa.setDireccion(direccion);
        empresaRepository.save(empresa);

        redirectAttributes.addFlashAttribute("exito", "Empresa editada correctamente.");
        return "redirect:/admin/empresas";
    }

    @PostMapping("/admin/empresas/eliminar")
    public String eliminarEmpresa(@RequestParam Integer idEmp,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Empresa empresa = empresaRepository.findById(idEmp)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));

        // Seguridad: el coordinador solo puede eliminar empresas de su propia especialidad
        if (empresa.getEspecialidad() == null || !empresa.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés eliminar una empresa de otra especialidad");
        }

        // Antes de borrar, desasignamos esta empresa de cualquier alumno que la tenga puesta,
        // porque la FK en la base no permite borrar una empresa que todavía está en uso.
        alumnoRepository.desasignarEmpresa(idEmp);

        empresaRepository.delete(empresa);

        redirectAttributes.addFlashAttribute("exito", "Empresa eliminada correctamente.");
        return "redirect:/admin/empresas";
    }

    @PostMapping("/admin/empresas/asignar")
    public String asignarEmpresa(@RequestParam Integer idAl,
                                  @RequestParam(required = false) Integer idEmp,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

        Administrador admin = obtenerAdminAutenticado(authentication);
        Especialidad especialidadFija = obtenerEspecialidadDeCoordinador(admin);

        Alumno alumno = alumnoRepository.findById(idAl)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        // Seguridad: el coordinador solo puede tocar alumnos de su propia especialidad
        if (!alumno.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
            throw new AccessDeniedException("No podés modificar un alumno de otra especialidad");
        }

        if (idEmp == null) {
            alumno.setEmpresa(null); // se puede desasignar eligiendo "-- Sin asignar --"
        } else {
            Empresa empresa = empresaRepository.findById(idEmp)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));

            if (empresa.getEspecialidad() == null || !empresa.getEspecialidad().getIdEsp().equals(especialidadFija.getIdEsp())) {
                throw new AccessDeniedException("Esa empresa no pertenece a tu especialidad");
            }

            alumno.setEmpresa(empresa);
        }

        alumnoRepository.save(alumno);

        redirectAttributes.addFlashAttribute("exito", "Empresa actualizada correctamente.");
        return "redirect:/admin/empresas";
    }

    private Alumno verificarAccesoAlumno(Integer idAl, Authentication authentication) {
        Administrador admin = obtenerAdminAutenticado(authentication);
        Alumno alumno = alumnoRepository.findById(idAl)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        Integer idEspecialidad = alumno.getEspecialidad() == null
                ? null
                : alumno.getEspecialidad().getIdEsp();
        if (idEspecialidad == null || !obtenerIdsEspecialidadPermitidos(admin).contains(idEspecialidad)) {
            throw new AccessDeniedException("No podés consultar documentos de esta especialidad");
        }

        return alumno;
    }

    private String sanitizar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    // Delega en el helper compartido para que exista una sola definicion de que es
    // un nombre de archivo seguro: cuando estaba escrita aca adentro, la vista del
    // alumno quedo sin sanitizar y nadie lo noto.
    private String sanitizarNombreArchivo(String nombre, Integer idDs) {
        return NombresDeArchivo.seguro(nombre, "documento_" + idDs + ".pdf");
    }

    private Administrador obtenerAdminAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return administradorRepository.findByUsuario_IdUsr(usuario.getIdUsr())
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado"));
    }
}