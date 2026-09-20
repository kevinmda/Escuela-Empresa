package com.EscuelaEmpresa.gestor_pasantes.dto.movil;

import java.time.LocalDateTime;

import com.EscuelaEmpresa.gestor_pasantes.dto.ChromeContext;

// Mismo aviso que ve la campana del header web (ChromeContext.Aviso), pero sin
// "destino" ni "tipo": destino es una ruta web (/alumno/documentos/...) y la
// app no tiene esas pantallas, y tipo es solo el color de la campana web, algo
// que la propia app decide con su color de acento. "clave" sí viaja: hace
// falta para marcar el aviso como leído después.
public class AvisoDTO {

    private final String texto;
    private final String codigo;
    private final String clave;
    private final LocalDateTime momento;
    private final boolean leido;

    public AvisoDTO(ChromeContext.Aviso aviso) {
        this.texto = aviso.texto();
        this.codigo = aviso.codigo();
        this.clave = aviso.clave();
        this.momento = aviso.momento();
        this.leido = aviso.leido();
    }

    public String getTexto() { return texto; }
    public String getCodigo() { return codigo; }
    public String getClave() { return clave; }
    public LocalDateTime getMomento() { return momento; }
    public boolean isLeido() { return leido; }
}
