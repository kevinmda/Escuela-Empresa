package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
//se tiene que crear una interfaz (en este caso "UsuarioRepository") que extiende de JpaRepository<T, ID>
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> { //JpaRepository<T, ID> es una interfaz generica que provee Spring Data JPA y que cuando tu interfaz la extiende (extends) hereda un monton de metodos CRUD (Crear, Leer, Actualizar, Borrar). Donde esta la "T" iria la Entidad que maneja el repositorio y donde esta "ID" va el tipo de dato de la clave primaria
    Optional<Usuario> findByEmail(String email); //Spring Data JPA se encarga de interpretar el nombre del metodo "findByEmail" y por detras genera automaticamente "WHERE email = ". Esto seria un metodo que nosotros creamos para filtrar por email. Es Optional<> para que en caso de no encontrar evitemos que nos de un error tipo NullPointerException

    // --- Contador de intentos de login, actualizado de forma atómica ---
    // Antes se hacía leer-modificar-guardar en Java, y dos intentos fallidos
    // simultáneos sobre la misma cuenta perdían un incremento (más intentos de los
    // permitidos antes del bloqueo). Estas dos sentencias lo hacen en la base.

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Usuario u SET u.intentosLogin = u.intentosLogin + 1 "
            + "WHERE u.email = :email AND u.activo = true")
    void incrementarIntentosLogin(@Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Usuario u SET u.bloqueadoHasta = :hasta, u.intentosLogin = 0 "
            + "WHERE u.email = :email AND u.intentosLogin >= :umbral")
    void bloquearSiSuperaIntentos(@Param("email") String email,
                                  @Param("hasta") LocalDateTime hasta,
                                  @Param("umbral") int umbral);
}