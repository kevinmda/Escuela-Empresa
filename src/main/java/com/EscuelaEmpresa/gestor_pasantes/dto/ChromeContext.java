package com.EscuelaEmpresa.gestor_pasantes.dto;

import java.util.List;

/**
 * Datos de identidad que necesita el cromo compartido de las pantallas autenticadas.
 * No expone la entidad de dominio completa a las vistas: el header solo necesita
 * saber quién está dentro, qué rol ocupa y, para un alumno, su especialidad.
 */
public record ChromeContext(
        String rol,
        String nombre,
        String email,
        Integer especialidadId,
        String especialidadNombre) {

    /**
     * Un destino del riel de secciones. La clave es la que compara paginaActual.
     * Cuando subsecciones no está vacía, el riel la dibuja como un desplegable
     * (mismo mecanismo de <details> que ya usa el menú de cuenta) en vez de un
     * enlace directo; destino queda sin uso en ese caso.
     */
    public record Seccion(String clave, String etiqueta, String destino, List<Seccion> subsecciones) {
        public Seccion(String clave, String etiqueta, String destino) {
            this(clave, etiqueta, destino, List.of());
        }

        public boolean tieneSubsecciones() {
            return subsecciones != null && !subsecciones.isEmpty();
        }

        /** Activa tanto si es la página actual como si lo es alguna de sus hijas. */
        public boolean activa(String paginaActual) {
            if (clave.equals(paginaActual)) {
                return true;
            }
            return subsecciones != null && subsecciones.stream().anyMatch(s -> s.clave().equals(paginaActual));
        }
    }

    /**
     * Las secciones se arman acá y no en la plantilla. Antes cada vista escribía
     * su propia lista de enlaces a mano, que es exactamente como Supervisores y
     * Empresas terminaron apareciendo en unas pantallas y en otras no. Agregar una
     * sección nueva ahora es una línea, y el riel de las once pantallas la toma.
     */
    public List<Seccion> secciones() {
        Seccion inicio = new Seccion("inicio", "Inicio", "/home");
        if (rol == null) {
            return List.of(inicio);
        }
        return switch (rol) {
            // Imprimir y Planilla Semanal se fusionan en un solo desplegable
            // "Documentos": separa los mismos documentos que antes vivían todos
            // juntos en /alumno/imprimir, más el acceso a las planillas y a los
            // adjuntos, sin agregar una sección nueva al riel.
            case "alumno" -> List.of(
                    inicio,
                    new Seccion("documentos", "Documentos", null, List.of(
                            new Seccion("documentos-antes", "Antes de empezar", "/alumno/documentos/antes-de-empezar"),
                            new Seccion("planilla", "Planillas Semanales", "/alumno/planilla"),
                            new Seccion("documentos-final", "Al terminar", "/alumno/documentos/al-terminar"),
                            new Seccion("documentos-informe", "Informe de Pasantía", "/alumno/documentos/informe"),
                            new Seccion("documentos-adjuntos", "Documentos adjuntos", "/alumno/documentos/adjuntos"))),
                    new Seccion("subir", "Subir", "/alumno/subir"));
            // Administrativo no ve Supervisores ni Empresas: es la misma regla que
            // antes vivía repartida en th:unless="${esAdministrativo}" por plantilla.
            case "administrador" -> List.of(
                    inicio,
                    new Seccion("alumnos", "Alumnos", "/admin/alumnos"),
                    new Seccion("reportes", "Reportes", "/admin/reportes"));
            case "coordinador" -> List.of(
                    inicio,
                    new Seccion("alumnos", "Alumnos", "/admin/alumnos"),
                    new Seccion("reportes", "Reportes", "/admin/reportes"),
                    new Seccion("supervisores", "Supervisores", "/admin/supervisores"),
                    new Seccion("empresas", "Empresas", "/admin/empresas"));
            default -> List.of(inicio);
        };
    }

    /**
     * Una o dos letras para el disco de la cuenta. Se calcula acá y no en la
     * plantilla porque en Thymeleaf saldría una cadena de #strings.substring
     * ilegible, y porque el caso de un solo nombre hay que resolverlo igual.
     */
    public String iniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        if (partes.length == 1) {
            return primera.toUpperCase();
        }
        return (primera + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    /** Cómo se nombra el rol de cara al usuario, que no piensa en las claves internas. */
    public String rolEtiqueta() {
        if (rol == null) {
            return "Cuenta";
        }
        return switch (rol) {
            case "alumno" -> "Alumno";
            case "administrador" -> "Administrativo";
            case "coordinador" -> "Coordinador";
            default -> "Cuenta";
        };
    }

    /**
     * El manual en PDF cambia por rol. Se devuelve con los espacios ya
     * codificados porque el nombre del archivo los tiene: es la misma cadena
     * literal que estaba escrita a mano en cada plantilla, así que th:href
     * la trata igual que antes.
     */
    public String pdfAyuda() {
        if (rol == null) {
            return null;
        }
        return switch (rol) {
            case "alumno" -> "/pdf/Escuela%20Empresa%20-%20Alumnos.pdf";
            case "administrador" -> "/pdf/Escuela%20Empresa%20-%20Administradores.pdf";
            case "coordinador" -> "/pdf/Escuela%20Empresa%20-%20Coordinadores.pdf";
            default -> null;
        };
    }
}
