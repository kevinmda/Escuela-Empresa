package com.EscuelaEmpresa.gestor_pasantes.repository;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
//se tiene que crear una interfaz (en este caso "UsuarioRepository") que extiende de JpaRepository<T, ID>
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> { //JpaRepository<T, ID> es una interfaz generica que provee Spring Data JPA y que cuando tu interfaz la extiende (extends) hereda un monton de metodos CRUD (Crear, Leer, Actualizar, Borrar). Donde esta la "T" iria la Entidad que maneja el repositorio y donde esta "ID" va el tipo de dato de la clave primaria
    Optional<Usuario> findByEmail(String email); //Spring Data JPA se encarga de interpretar el nombre del metodo "findByEmail" y por detras genera automaticamente "WHERE email = ". Esto seria un metodo que nosotros creamos para filtrar por email. Es Optional<> para que en caso de no encontrar evitemos que nos de un error tipo NullPointerException
}