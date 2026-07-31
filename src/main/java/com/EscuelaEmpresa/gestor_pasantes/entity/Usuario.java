package com.EscuelaEmpresa.gestor_pasantes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity                     //esto le dice a Hibernate que la clase representa una tabla, sin esto va a ser un java normal para JPA
@Table(name = "usuario")    //esto indica especificamente a que tabla nos estamos refiriendo
public class Usuario {      //La Clase se llama Usuario con mayuscula para evitar problemas con el nombre real de la tabla

    @Id                                                       //esto es para decir que columna es la clave primaria
    @Column(name = "id_Usr")                                  //se especifica que columna es la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)       //le dice "no generes vos el ID en Java, dejá que MySQL lo genere automáticamente" (el típico AUTO_INCREMENT)
    private Integer idUsr;                                    //la manera que se suele nombrar variables (convención camelCase)
    //atributos
    private String ci;
    private String contrasena;
    private String email;
    private Boolean activo;

    @Column(name = "token_activacion", length = 100)
    private String tokenActivacion;

    @Column(name = "token_expiracion")
    private LocalDateTime tokenExpiracion;

    public Usuario() {} //constructor vacio, necesario porque internamente se crea un objeto vacio que luego recien se va llenando
    //getter y los setter. Estos son usados activamente por Hibernate para leer y escribir los valores de cada campo al convertir entre el objeto Java y la fila SQL
    public Integer getIdUsr() { return idUsr; }
    public void setIdUsr(Integer idUsr) { this.idUsr = idUsr; }

    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public String getTokenActivacion() { return tokenActivacion; }
    public void setTokenActivacion(String tokenActivacion) { this.tokenActivacion = tokenActivacion; }

    public LocalDateTime getTokenExpiracion() { return tokenExpiracion; }
    public void setTokenExpiracion(LocalDateTime tokenExpiracion) { this.tokenExpiracion = tokenExpiracion; }
}
