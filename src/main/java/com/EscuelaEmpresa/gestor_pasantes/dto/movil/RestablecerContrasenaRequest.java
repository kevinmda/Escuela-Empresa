package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

public class RestablecerContrasenaRequest {

    private String email;
    private String codigo;
    private String nuevaContrasena;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNuevaContrasena() { return nuevaContrasena; }
    public void setNuevaContrasena(String nuevaContrasena) { this.nuevaContrasena = nuevaContrasena; }
}
