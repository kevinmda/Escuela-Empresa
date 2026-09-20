package com.EscuelaEmpresa.gestor_pasantes.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.dto.ChromeContext;
import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.AvisoLeido;
import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.exception.ReglaNegocioException;
import com.EscuelaEmpresa.gestor_pasantes.repository.AvisoLeidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;

// Los avisos del header web (ver ChromeContext.Aviso / ChromeModelAdvice) y su
// equivalente en la app Android (ver AvisosMovilController) comparten esta
// misma lógica -- antes vivía duplicada solo del lado web, y una app aparte
// consumiendo la misma regla de negocio es exactamente el caso para sacarla a
// un service en vez de copiarla.
@Service
public class AvisoService {

    private static final List<String> CODIGOS_VALIDOS = List.of("documentos_finales", "documentos_adjuntos");

    private final PlanillaSemanalRepository planillaSemanalRepository;
    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final LimitesDocumentoService limitesDocumentoService;
    private final AvisoLeidoRepository avisoLeidoRepository;

    public AvisoService(PlanillaSemanalRepository planillaSemanalRepository,
                         DocumentoSubidoRepository documentoSubidoRepository,
                         LimitesDocumentoService limitesDocumentoService,
                         AvisoLeidoRepository avisoLeidoRepository) {
        this.planillaSemanalRepository = planillaSemanalRepository;
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.limitesDocumentoService = limitesDocumentoService;
        this.avisoLeidoRepository = avisoLeidoRepository;
    }

    // Los dos avisos posibles hoy pueden convivir (uno depende de las planillas
    // cargadas en el sistema, el otro de los documentos subidos como PDF, son
    // cosas independientes), por eso es una lista real y no un par de casos
    // sueltos. Las dos condiciones se vuelven a evaluar en cada llamada desde
    // el estado actual (cuántas planillas/documentos hay ahora mismo) -- si se
    // borra una planilla o un documento y la condición deja de cumplirse, el
    // aviso correspondiente deja de aparecer solo, sin que haga falta borrar
    // nada de aviso_leido.
    public List<ChromeContext.Aviso> avisosDelAlumno(Alumno alumno) {
        List<PlanillaSemanal> planillas =
                planillaSemanalRepository.findByAlumno_IdAlOrderByFechaDesdeDesc(alumno.getIdAl());
        List<ChromeContext.Aviso> avisos = new ArrayList<>();

        if (planillas.size() >= 6) {
            // findByAlumno_IdAlOrderByFechaDesdeDesc: la más nueva (la sexta) va
            // primera.
            PlanillaSemanal sextaPlanilla = planillas.get(0);
            LocalDate fechaSextaPlanilla = sextaPlanilla.getFechaHasta();
            // La clave es el id de la planilla, no su fecha_hasta: si se borra
            // la sexta y se vuelve a cargar con las MISMAS fechas de semana
            // (lo más probable al reintentar, sobre todo probando), fecha_hasta
            // da igual que antes y el aviso seguía saliendo como ya leído --
            // exactamente el mismo problema que ya se corrigió para
            // documentos_adjuntos, pero con el id de planilla en vez del id de
            // documento. El id, en cambio, es autoincremental: la planilla
            // nueva SIEMPRE tiene uno distinto al de la que se borró, aunque
            // elijas la semana con las mismas fechas.
            String clave = String.valueOf(sextaPlanilla.getIdPs());
            // fecha_hasta la elige el alumno al cargar la planilla, y puede caer
            // después de hoy (una semana de prueba con fecha futura, por
            // ejemplo) -- sin este límite, ese aviso terminaba ordenándose como
            // "más reciente" que una subida real de hoy en documentos_adjuntos,
            // aunque en el mundo real no haya pasado todavía. min() lo acota a
            // hoy como máximo, nunca antes de tiempo. Zona horaria explícita,
            // el servidor puede no estar configurado en hora de Paraguay.
            LocalDate hoyEnParaguay = LocalDate.now(ZoneId.of("America/Asuncion"));
            LocalDateTime momento = fechaSextaPlanilla.isAfter(hoyEnParaguay)
                    ? hoyEnParaguay.atStartOfDay()
                    : fechaSextaPlanilla.atStartOfDay();
            avisos.add(new ChromeContext.Aviso(
                    "Ya se habilitaron tus documentos finales",
                    "/alumno/documentos/al-terminar",
                    "listo",
                    "documentos_finales",
                    clave,
                    momento,
                    yaLeido(alumno, "documentos_finales", clave)));
        }

        // Documentos Adjuntos (el expediente combinado, en /alumno/documentos/adjuntos)
        // y su caja de subida en Subir se habilitan con los 10 comprobantes ya
        // subidos como PDF -- algo aparte e independiente de las planillas
        // cargadas en el sistema arriba. Una vez que el propio combinado ya se
        // subió de vuelta, el aviso deja de tener sentido (puedeSubirAdjuntos en
        // SubirController usa exactamente esta misma condición para ocultar esa
        // caja), así que tampoco se muestra en ese caso.
        List<DocumentoSubido> documentos =
                documentoSubidoRepository.findByAlumno_IdAlOrderByFechaSubidaDesc(alumno.getIdAl());
        boolean yaSubioAdjuntos = documentos.stream()
                .anyMatch(d -> d.getTipoDocumento() == TipoDocumento.DOCUMENTOS_ADJUNTOS);
        long totalSubidos = limitesDocumentoService.contarTotalSubidos(alumno.getIdAl());
        int totalLimite = limitesDocumentoService.obtenerLimiteTotal();

        if (totalSubidos >= totalLimite && !yaSubioAdjuntos) {
            // El de mayor id (no el de fecha_subida más reciente) es, por
            // construcción, el que completó el umbral: fecha_subida es un
            // DATETIME sin fracción de segundo en la base, así que dos
            // documentos subidos dentro del mismo segundo (por ejemplo,
            // eliminar uno y subir el reemplazo enseguida, como al probar)
            // pueden guardar la MISMA fecha exacta -- "el más reciente por
            // fecha" queda ambiguo justo en ese caso. El id, en cambio, es
            // autoincremental y siempre estrictamente creciente, así que no
            // tiene ese problema.
            documentos.stream()
                    .filter(d -> d.getTipoDocumento() != TipoDocumento.DOCUMENTOS_ADJUNTOS)
                    .max(Comparator.comparing(DocumentoSubido::getIdDs))
                    .ifPresent(masReciente -> {
                        String clave = String.valueOf(masReciente.getIdDs());
                        avisos.add(new ChromeContext.Aviso(
                                "Ya se habilitó el expediente combinado en Documentos adjuntos y su caja de subida en Subir",
                                "/alumno/documentos/adjuntos",
                                "listo",
                                "documentos_adjuntos",
                                clave,
                                masReciente.getFechaSubida(),
                                yaLeido(alumno, "documentos_adjuntos", clave)));
                    });
        }

        return avisos;
    }

    public void marcarLeido(Alumno alumno, String codigo, String clave) {
        if (!CODIGOS_VALIDOS.contains(codigo)) {
            throw new ReglaNegocioException("Código de aviso inválido");
        }

        // Una fila por (alumno, código): si ya había una de un aviso anterior de
        // este mismo código, se pisa con la clave nueva en vez de acumular
        // historial que nadie necesita leer.
        AvisoLeido avisoLeido = avisoLeidoRepository.findByIdAlAndCodigo(alumno.getIdAl(), codigo)
                .orElseGet(AvisoLeido::new);
        avisoLeido.setIdAl(alumno.getIdAl());
        avisoLeido.setCodigo(codigo);
        avisoLeido.setClave(clave);
        avisoLeidoRepository.save(avisoLeido);
    }

    private boolean yaLeido(Alumno alumno, String codigo, String clave) {
        return avisoLeidoRepository.findByIdAlAndCodigo(alumno.getIdAl(), codigo)
                .map(leido -> clave.equals(leido.getClave()))
                .orElse(false);
    }
}
