package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.DocumentoSubido;
import com.EscuelaEmpresa.gestor_pasantes.entity.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoSubidoRepository extends JpaRepository<DocumentoSubido, Integer> {
    List<DocumentoSubido> findByAlumno_IdAlOrderByFechaSubidaDesc(Integer idAl);

    // Version paginada del mismo listado, para la API movil (GET /api/movil/documentos?page=&size=)
    Page<DocumentoSubido> findByAlumno_IdAlOrderByFechaSubidaDesc(Integer idAl, Pageable pageable);

    // Contar documentos por alumno y tipo
    long countByAlumno_IdAlAndTipoDocumento(Integer idAl, TipoDocumento tipoDocumento);
}
