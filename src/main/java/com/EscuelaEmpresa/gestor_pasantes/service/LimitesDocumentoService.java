package com.EscuelaEmpresa.gestor_pasantes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalRepository;

/**
 * Servicio para gestionar los límites de subida de documentos por tipo.
 *
 * Límites:
 * - PLANTILLA_SEMANAL: hasta 6 subidas; se exigen 5 como mínimo, o 6 si el
 *   alumno cargó una sexta semana en el sistema
 * - Otros tipos: 1 subida máximo
 */
@Service
public class LimitesDocumentoService {

    // Los cinco tipos que forman el expediente de 10 comprobantes.
    // DOCUMENTOS_ADJUNTOS queda afuera a propósito: es el expediente ya
    // combinado y firmado, y solo tiene sentido subirlo una vez que estos
    // cinco ya están completos, así que no cuenta para el total ni para
    // "expedienteCompleto".
    private static final List<TipoDocumento> TIPOS_EXPEDIENTE = List.of(
            TipoDocumento.CONTRATO,
            TipoDocumento.AUTORIZACION,
            TipoDocumento.PLANTILLA_SEMANAL,
            TipoDocumento.FICHA_FINAL_ALUMNO,
            TipoDocumento.FICHA_FINAL_EVALUATIVA
    );

    private final DocumentoSubidoRepository documentoSubidoRepository;
    private final PlanillaSemanalRepository planillaSemanalRepository;

    public LimitesDocumentoService(DocumentoSubidoRepository documentoSubidoRepository,
                                   PlanillaSemanalRepository planillaSemanalRepository) {
        this.documentoSubidoRepository = documentoSubidoRepository;
        this.planillaSemanalRepository = planillaSemanalRepository;
    }

    /**
     * Obtiene el límite máximo de subidas para un tipo de documento
     */
    public int obtenerLimitePorTipo(TipoDocumento tipo) {
        if (tipo == TipoDocumento.PLANTILLA_SEMANAL) {
            return PlanillaSemanalService.MAX_SEMANAS; // hasta 6 planillas
        }
        return 1; // Todos los otros tipos permiten solo 1
    }

    /**
     * Cuántos comprobantes de un tipo se le exigen a este alumno para dar el
     * expediente por completo. Para las planillas son 5 como mínimo; si cargó
     * una sexta semana en el sistema, también hay que entregar su comprobante.
     * Los demás tipos siempre son 1.
     */
    public int obtenerRequeridoPorTipo(Integer idAlumno, TipoDocumento tipo) {
        if (tipo == TipoDocumento.PLANTILLA_SEMANAL) {
            long cargadas = planillaSemanalRepository.countByAlumno_IdAl(idAlumno);
            return (int) Math.max(PlanillaSemanalService.MIN_SEMANAS,
                    Math.min(cargadas, PlanillaSemanalService.MAX_SEMANAS));
        }
        return obtenerLimitePorTipo(tipo);
    }

    /**
     * Cuenta cuántos documentos de un tipo ya subió el alumno
     */
    public long contarDocumentosSubidos(Integer idAlumno, TipoDocumento tipo) {
        return documentoSubidoRepository.countByAlumno_IdAlAndTipoDocumento(idAlumno, tipo);
    }

    /**
     * Verifica si el alumno puede subir un documento de este tipo
     * 
     * @return true si puede subir, false si ya alcanzó el límite
     */
    public boolean puedeSubirDocumento(Integer idAlumno, TipoDocumento tipo) {
        long subidos = contarDocumentosSubidos(idAlumno, tipo);
        // Se compara contra lo que se le exige a este alumno, no contra el máximo
        // del tipo: con 5 semanas cargadas hay lugar para 5 comprobantes de
        // planilla; el sexto se habilita recién cuando cargue esa sexta semana.
        int limite = obtenerRequeridoPorTipo(idAlumno, tipo);
        return subidos < limite;
    }

    /**
     * Los tipos que se suben en la grilla principal de /alumno/subir: todos
     * menos DOCUMENTOS_ADJUNTOS, que tiene su propio apartado exclusivo.
     */
    public List<TipoDocumento> obtenerTiposExpediente() {
        return TIPOS_EXPEDIENTE;
    }

    /**
     * Verifica si el alumno ya entregó el expediente completo: una autorización,
     * un contrato, sus planillas semanales (5 o 6), una ficha final y una ficha
     * final evaluativa, o sea 9 o 10 comprobantes. Es la condición que
     * habilita imprimir/descargar todo junto en un solo PDF, y también la que
     * habilita subir DOCUMENTOS_ADJUNTOS.
     */
    public boolean expedienteCompleto(Integer idAlumno) {
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            if (contarDocumentosSubidos(idAlumno, tipo) < obtenerRequeridoPorTipo(idAlumno, tipo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cuántos comprobantes componen el expediente completo de este alumno: 9 si
     * tiene 5 semanas (autorización + contrato + 5 planillas + las dos fichas) o
     * 10 si cargó una sexta.
     */
    public int obtenerLimiteTotal(Integer idAlumno) {
        int total = 0;
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            total += obtenerRequeridoPorTipo(idAlumno, tipo);
        }
        return total;
    }

    /**
     * Cuántos comprobantes ya subió el alumno en total, sumando los cinco tipos
     * del expediente (no incluye DOCUMENTOS_ADJUNTOS). Cada tipo cuenta como
     * mucho lo que se le exige: así "totalSubidos >= obtenerLimiteTotal" equivale
     * a expediente completo y el contador nunca muestra más de lo pedido.
     */
    public long contarTotalSubidos(Integer idAlumno) {
        long total = 0;
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            total += Math.min(contarDocumentosSubidos(idAlumno, tipo), obtenerRequeridoPorTipo(idAlumno, tipo));
        }
        return total;
    }

    /**
     * Obtiene un mensaje de error descriptivo cuando el alumno alcanzó el límite
     */
    public String obtenerMensajeError(TipoDocumento tipo) {
        if (tipo == TipoDocumento.PLANTILLA_SEMANAL) {
            return "Ya subiste un comprobante por cada semana de planilla que cargaste (máximo "
                   + PlanillaSemanalService.MAX_SEMANAS + "). Para subir otro, primero cargá esa semana en Planilla Semanal.";
        }
        int limite = obtenerLimitePorTipo(tipo);
        if (limite == 1) {
            return "Ya subiste el máximo de 1 documento de tipo '" + tipo.getDescripcion() 
                   + "'. Para subir otro, debes eliminar el anterior.";
        }
        return "Ya alcanzaste el límite de " + limite + " documentos de tipo '" 
               + tipo.getDescripcion() + "'.";
    }
}
