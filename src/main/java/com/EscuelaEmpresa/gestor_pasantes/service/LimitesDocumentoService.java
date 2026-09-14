package com.EscuelaEmpresa.gestor_pasantes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import com.EscuelaEmpresa.gestor_pasantes.repository.DocumentoSubidoRepository;

/**
 * Servicio para gestionar los límites de subida de documentos por tipo.
 *
 * Límites:
 * - PLANTILLA_SEMANAL: 6 subidas máximo
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
            TipoDocumento.AUTORIZACION,
            TipoDocumento.CONTRATO,
            TipoDocumento.PLANTILLA_SEMANAL,
            TipoDocumento.FICHA_FINAL_ALUMNO,
            TipoDocumento.FICHA_FINAL_EVALUATIVA
    );

    private final DocumentoSubidoRepository documentoSubidoRepository;

    public LimitesDocumentoService(DocumentoSubidoRepository documentoSubidoRepository) {
        this.documentoSubidoRepository = documentoSubidoRepository;
    }

    /**
     * Obtiene el límite máximo de subidas para un tipo de documento
     */
    public int obtenerLimitePorTipo(TipoDocumento tipo) {
        if (tipo == TipoDocumento.PLANTILLA_SEMANAL) {
            return 6; // Plantilla semanal permite 6 subidas
        }
        return 1; // Todos los otros tipos permiten solo 1
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
        int limite = obtenerLimitePorTipo(tipo);
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
     * Verifica si el alumno ya entregó el expediente completo: los diez
     * comprobantes (una autorización, un contrato, seis planillas semanales,
     * una ficha final y una ficha final evaluativa). Es la condición que
     * habilita imprimir/descargar todo junto en un solo PDF, y también la que
     * habilita subir DOCUMENTOS_ADJUNTOS.
     */
    public boolean expedienteCompleto(Integer idAlumno) {
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            if (contarDocumentosSubidos(idAlumno, tipo) < obtenerLimitePorTipo(tipo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cuántos comprobantes en total componen el expediente completo (hoy son 10:
     * autorización + contrato + 6 planillas + ficha final + ficha final evaluativa).
     */
    public int obtenerLimiteTotal() {
        int total = 0;
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            total += obtenerLimitePorTipo(tipo);
        }
        return total;
    }

    /**
     * Cuántos comprobantes ya subió el alumno en total, sumando los cinco tipos
     * del expediente (no incluye DOCUMENTOS_ADJUNTOS).
     */
    public long contarTotalSubidos(Integer idAlumno) {
        long total = 0;
        for (TipoDocumento tipo : TIPOS_EXPEDIENTE) {
            total += contarDocumentosSubidos(idAlumno, tipo);
        }
        return total;
    }

    /**
     * Obtiene un mensaje de error descriptivo cuando el alumno alcanzó el límite
     */
    public String obtenerMensajeError(TipoDocumento tipo) {
        int limite = obtenerLimitePorTipo(tipo);
        if (limite == 1) {
            return "Ya subiste el máximo de 1 documento de tipo '" + tipo.getDescripcion() 
                   + "'. Para subir otro, debes eliminar el anterior.";
        }
        return "Ya alcanzaste el límite de " + limite + " documentos de tipo '" 
               + tipo.getDescripcion() + "'.";
    }
}
