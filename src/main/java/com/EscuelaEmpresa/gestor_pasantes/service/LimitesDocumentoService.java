package com.EscuelaEmpresa.gestor_pasantes.service;

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
     * Obtiene la descripción del límite para mostrar al usuario
     */
    public String obtenerDescripcionLimite(TipoDocumento tipo) {
        int limite = obtenerLimitePorTipo(tipo);
        if (limite == 6) {
            return "Máximo 6 subidas";
        }
        return "Máximo 1 subida";
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

    /**
     * Obtiene un mensaje informativo con el estado actual
     */
    public String obtenerMensajeInfo(Integer idAlumno, TipoDocumento tipo) {
        long subidos = contarDocumentosSubidos(idAlumno, tipo);
        int limite = obtenerLimitePorTipo(tipo);
        return "Has subido " + subidos + " de " + limite + " documentos de tipo '" 
               + tipo.getDescripcion() + "'.";
    }
}
