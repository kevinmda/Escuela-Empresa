package com.EscuelaEmpresa.gestor_pasantes.service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// Rellena la plantilla "Informe_Pasantia_Plantilla.docx" usando los BOOKMARKS que ya tiene
// armados el documento (Insertar > Marcador en Word). Cada bookmark (ej: "curso", "primer_lunes")
// envuelve un fragmento de texto de ejemplo dentro de un parrafo, y nosotros lo ubicamos
// buscando el nombre del bookmark en el XML de cada parrafo con documento.getParagraphs().
//
// NOTA: la plantilla original tenia el bloque de la portada (Alumno/Curso/Turno/etc.) metido
// dentro de un cuadro de texto flotante (representado en el XML como <mc:AlternateContent>
// con dos versiones alternativas del mismo contenido: una moderna y una de compatibilidad
// vieja). Apache POI no tiene soporte de alto nivel para editar texto dentro de cuadros de
// texto, asi que la plantilla se modifico una sola vez (a mano, fuera de esta app) para sacar
// esos campos del cuadro de texto y dejarlos como parrafos normales del documento -- por eso
// esta clase puede usar simplemente documento.getParagraphs(), igual que para las semanas y dias.
@Service
public class InformePasantiaService {

    private final PlanillaSemanalDetalleRepository detalleRepository;

    // "femenino" para las semanas (primera, segunda...) y "masculino" para los dias (primer, segundo...)
    // porque asi estan armados los nombres de los bookmarks en la plantilla
    private static final String[] ORDINAL_SEMANA = {"primera", "segunda", "tercera", "cuarta", "quinta", "sexta"};
    private static final String[] ORDINAL_DIA = {"primer", "segundo", "tercer", "cuarto", "quinto", "sexto"};

    private static final DayOfWeek[] DIAS_SEMANA = {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    };
    private static final String[] NOMBRE_BOOKMARK_DIA = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado"};
    private static final String[] NOMBRE_DIA_TEXTO = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sabado"};

    private static final Locale LOCALE_ES = new Locale.Builder().setLanguage("es").setRegion("ES").build();
    private static final DateTimeFormatter FORMATO_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");

    public InformePasantiaService(PlanillaSemanalDetalleRepository detalleRepository) {
        this.detalleRepository = detalleRepository;
    }

    /**
     * @param planillasOrdenadas las planillas del alumno, ordenadas de la semana MAS ANTIGUA a la MAS NUEVA
     */
    public XWPFDocument generarInforme(Alumno alumno, List<PlanillaSemanal> planillasOrdenadas) throws IOException {

        XWPFDocument documento;
        try (FileInputStream fis = new FileInputStream(new File("src/main/resources/plantillas/Informe_Pasantia_Plantilla.docx"))) {
            documento = new XWPFDocument(fis);
        }

        completarEncabezado(documento, alumno, planillasOrdenadas);
        completarSemanas(documento, planillasOrdenadas);

        return documento;
    }

    private void completarEncabezado(XWPFDocument documento, Alumno alumno, List<PlanillaSemanal> planillas) {

        boolean esMujer = "Femenino".equalsIgnoreCase(alumno.getSexo());
        String nombreCompleto = alumno.getNombres() + " " + alumno.getApellidos();
        reemplazarParrafoCompleto(documento, "alumno", (esMujer ? "Alumna: " : "Alumno: ") + nombreCompleto);

        reemplazarDentroDeParrafo(documento, "curso", "Curso", alumno.getCurso());
        reemplazarDentroDeParrafo(documento, "turno", "Turno", calcularTurno(alumno.getSeccion()));
        reemplazarDentroDeParrafo(documento, "especialidad", "Especialidad",
                alumno.getEspecialidad() != null ? alumno.getEspecialidad().getNombre() : "");
        reemplazarDentroDeParrafo(documento, "empresa", "Empresa",
                alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : "");
        reemplazarDentroDeParrafo(documento, "docente", "Docente/Tutor",
                alumno.getSupervisor() != null
                        ? alumno.getSupervisor().getNombres() + " " + alumno.getSupervisor().getApellidos()
                        : "");

        // "Tutor de pasantia" es el campo de texto libre que el alumno escribio en su planilla semanal
        // (tabla planilla_semanal, no la tabla supervisor) - tomamos el de la planilla mas reciente
        String tutorPasantia = planillas.isEmpty() ? "" : planillas.get(planillas.size() - 1).getSupervisor();
        reemplazarDentroDeParrafo(documento, "tutor_pasantia", "Tutor de pasantía", tutorPasantia);

        reemplazarParrafoCompleto(documento, "pasantia_empresa",
                alumno.getEmpresa() != null ? alumno.getEmpresa().getNombre() : "");

        if (!planillas.isEmpty()) {
            LocalDate inicio = planillas.get(0).getFechaDesde();
            LocalDate fin = planillas.get(planillas.size() - 1).getFechaHasta();
            reemplazarDentroDeParrafo(documento, "pasantia_periodo", "Periodo", formatearPeriodo(inicio, fin));
        }
    }

    private void completarSemanas(XWPFDocument documento, List<PlanillaSemanal> planillas) {

        int cantidad = Math.min(planillas.size(), 6);

        for (int semana = 0; semana < cantidad; semana++) {
            PlanillaSemanal planilla = planillas.get(semana);
            List<PlanillaSemanalDetalle> detalles = detalleRepository.findByPlanillaSemanal_IdPs(planilla.getIdPs());

            String bookmarkFecha = ORDINAL_SEMANA[semana] + "_semana_fecha";
            reemplazarParrafoCompleto(documento, bookmarkFecha,
                    formatearTituloSemana(semana + 1, planilla.getFechaDesde(), planilla.getFechaHasta()));

            for (int dia = 0; dia < 6; dia++) {
                String bookmarkDia = ORDINAL_DIA[semana] + "_" + NOMBRE_BOOKMARK_DIA[dia];
                DayOfWeek diaSemana = DIAS_SEMANA[dia];

                PlanillaSemanalDetalle detalleDelDia = detalles.stream()
                        .filter(d -> d.getFecha().getDayOfWeek() == diaSemana)
                        .findFirst()
                        .orElse(null);

                String textoFinal;
                if (detalleDelDia != null) {
                    String fecha = detalleDelDia.getFecha().format(FORMATO_FECHA_CORTA);
                    textoFinal = NOMBRE_DIA_TEXTO[dia] + " " + fecha + ": " + detalleDelDia.getDescripcion();
                } else {
                    textoFinal = NOMBRE_DIA_TEXTO[dia] + ": Sin actividades registradas";
                }

                reemplazarParrafoCompleto(documento, bookmarkDia, textoFinal);
            }
        }

        eliminarSemanasSobrantes(documento, cantidad);
    }

    private String calcularTurno(String seccion) {
        if (seccion == null) return "";
        if (seccion.trim().equalsIgnoreCase("1ra")) return "Mañana";
        return "Tarde"; // 2da, 3ra
    }

    private String formatearPeriodo(LocalDate inicio, LocalDate fin) {
        DateTimeFormatter formatoInicio = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES);
        DateTimeFormatter formatoFin = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES);
        return inicio.format(formatoInicio) + " al " + fin.format(formatoFin);
    }

    // Formato pedido: "Semana N (D AL D DE MES DE AAAA)", ej: "Semana 1 (6 AL 6 DE JULIO DE 2026)"
    private String formatearTituloSemana(int numeroSemana, LocalDate desde, LocalDate hasta) {
        DateTimeFormatter formatoMes = DateTimeFormatter.ofPattern("MMMM", LOCALE_ES);
        String mes = hasta.format(formatoMes).toUpperCase(LOCALE_ES);
        return "SEMANA " + numeroSemana + " (" + desde.getDayOfMonth() + " AL " + hasta.getDayOfMonth()
                + " DE " + mes + " DE " + hasta.getYear() + ")";
    }

    // ---------- Manipulacion de bookmarks ----------

    private XWPFParagraph buscarParrafoPorBookmark(XWPFDocument documento, String bookmarkName) {
        String patron = "w:name=\"" + bookmarkName + "\"";
        for (XWPFParagraph parrafo : documento.getParagraphs()) {
            if (parrafo.getCTP().xmlText().contains(patron)) {
                return parrafo;
            }
        }
        return null;
    }

    private XWPFParagraph buscarParrafoConTexto(XWPFDocument documento, String texto) {
        for (XWPFParagraph parrafo : documento.getParagraphs()) {
            if (parrafo.getText() != null && parrafo.getText().contains(texto)) {
                return parrafo;
            }
        }
        return null;
    }

    // Para bookmarks donde TODO el parrafo es el marcador (ej: "Alumno: Alumno" completo,
    // o las fechas de semana/dia): se borran todos los runs del parrafo y se pone uno solo con el texto nuevo
    private void reemplazarParrafoCompleto(XWPFDocument documento, String bookmarkName, String textoNuevo) {
        XWPFParagraph parrafo = buscarParrafoPorBookmark(documento, bookmarkName);
        if (parrafo == null) return; // si no se encuentra el bookmark, no rompemos nada, seguimos de largo

        List<XWPFRun> runs = parrafo.getRuns();
        if (runs.isEmpty()) {
            parrafo.createRun().setText(textoNuevo);
            return;
        }
        runs.get(0).setText(textoNuevo, 0);
        for (int i = runs.size() - 1; i >= 1; i--) {
            parrafo.removeRun(i);
        }
    }

    // Para bookmarks donde el marcador es solo UNA PARTE del parrafo (ej: "Curso: " queda fijo,
    // solo "Curso" -el valor- se reemplaza): buscamos la secuencia de runs cuyo texto concatenado
    // coincide exactamente con el texto original conocido, y reemplazamos solo esos runs
    private void reemplazarDentroDeParrafo(XWPFDocument documento, String bookmarkName, String textoOriginal, String textoNuevo) {
        XWPFParagraph parrafo = buscarParrafoPorBookmark(documento, bookmarkName);
        if (parrafo == null) return;

        List<XWPFRun> runs = parrafo.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            StringBuilder acumulado = new StringBuilder();
            for (int j = i; j < runs.size(); j++) {
                String textoRun = runs.get(j).getText(0);
                acumulado.append(textoRun == null ? "" : textoRun);

                if (acumulado.toString().equals(textoOriginal)) {
                    runs.get(i).setText(textoNuevo, 0);
                    for (int k = j; k > i; k--) {
                        parrafo.removeRun(k);
                    }
                    return;
                }
                if (acumulado.length() > textoOriginal.length()) break;
            }
        }
    }

    // Borra TODO el bloque de semanas que el alumno no completo de una sola vez (desde la
    // primera semana sobrante hasta CONCLUSIONES), en vez de semana por semana -- borrar de
    // a una traia problemas porque al buscar el limite de una semana usando el bookmark de
    // la semana siguiente, esa siguiente semana ya podia estar borrada del paso anterior
    private void eliminarSemanasSobrantes(XWPFDocument documento, int cantidadPlanillas) {
        if (cantidadPlanillas >= 6) return;

        XWPFParagraph parrafoInicio = buscarParrafoPorBookmark(documento, ORDINAL_SEMANA[cantidadPlanillas] + "_semana_fecha");
        if (parrafoInicio == null) return;
        XWPFParagraph parrafoFin = buscarParrafoConTexto(documento, "CONCLUSIONES");

        List<?> elementos = documento.getBodyElements();
        int posInicio = elementos.indexOf(parrafoInicio);
        int posFin = parrafoFin != null ? elementos.indexOf(parrafoFin) : elementos.size();

        if (posInicio == -1) return;

        // de atras hacia adelante, para no invalidar los indices ya calculados al ir borrando
        for (int i = posFin - 1; i >= posInicio; i--) {
            documento.removeBodyElement(i);
        }
    }
}
