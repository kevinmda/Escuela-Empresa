package com.EscuelaEmpresa.gestor_pasantes.service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Rellena la plantilla "Informe_Pasantia_Plantilla.docx" usando los BOOKMARKS que ya tiene
// armados el documento (Insertar > Marcador en Word). Cada bookmark (ej: "curso", "primer_lunes")
// envuelve un fragmento de texto de ejemplo dentro de un parrafo.
//
// OJO: el bloque de la portada (Alumno/Curso/Turno/etc.) esta metido dentro de DOS cuadros de
// texto superpuestos en la plantilla (por eso cada uno de esos 7 campos tiene el bookmark
// duplicado dos veces). Los parrafos de un cuadro de texto NO aparecen en
// documento.getParagraphs() (esa funcion solo devuelve parrafos "de primer nivel"), asi que
// hay que recorrer el XML completo a mano para encontrarlos (ver obtenerTodosLosParrafos).
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

        reemplazarDentroDeParrafo(documento, "curso", "Curso", alumno.getCurso() + " " + alumno.getSeccion());
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
                    formatearRangoSemana(planilla.getFechaDesde(), planilla.getFechaHasta()));

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

    private String formatearRangoSemana(LocalDate desde, LocalDate hasta) {
        DateTimeFormatter formatoMes = DateTimeFormatter.ofPattern("MMMM", LOCALE_ES);
        String mes = hasta.format(formatoMes).toUpperCase(LOCALE_ES);
        return "(" + desde.getDayOfMonth() + " AL " + hasta.getDayOfMonth() + " DE " + mes + " DE " + hasta.getYear() + ")";
    }

    // ---------- Manipulacion de bookmarks ----------

    // Recorre TODOS los <w:p> del documento, incluyendo los que estan anidados dentro de
    // cuadros de texto/drawings (documento.getParagraphs() NO los devuelve, solo trae los
    // parrafos "de primer nivel" del cuerpo del documento)
    private List<XWPFParagraph> obtenerTodosLosParrafos(XWPFDocument documento) {
        List<XWPFParagraph> resultado = new ArrayList<>();
        XmlCursor cursor = documento.getDocument().getBody().newCursor();
        cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:p");
        while (cursor.hasNextSelection()) {
            cursor.toNextSelection();
            XmlObject obj = cursor.getObject();
            if (obj instanceof CTP) {
                resultado.add(new XWPFParagraph((CTP) obj, documento));
            }
        }
        cursor.dispose();
        return resultado;
    }

    // Devuelve TODOS los parrafos REALES que contienen ese bookmark (puede haber mas de uno,
    // ya que 7 de los campos de la portada estan duplicados en dos cuadros de texto).
    // Filtra los parrafos "contenedores" vacios que envuelven un cuadro de texto: esos tienen
    // el nombre del bookmark en su XML completo (por los hijos anidados) pero su texto PROPIO
    // esta vacio, asi que no son el parrafo real a modificar (modificarlos rompe el cuadro de texto)
    private List<XWPFParagraph> parrafosPorBookmark(XWPFDocument documento, String bookmarkName) {
        String patron = "w:name=\"" + bookmarkName + "\"";
        List<XWPFParagraph> resultado = new ArrayList<>();
        for (XWPFParagraph parrafo : obtenerTodosLosParrafos(documento)) {
            String texto = parrafo.getText();
            if (parrafo.getCTP().xmlText().contains(patron) && texto != null && !texto.trim().isEmpty()) {
                resultado.add(parrafo);
            }
        }
        return resultado;
    }

    private XWPFParagraph buscarParrafoConTexto(XWPFDocument documento, String texto) {
        for (XWPFParagraph parrafo : obtenerTodosLosParrafos(documento)) {
            if (parrafo.getText() != null && parrafo.getText().contains(texto)) {
                return parrafo;
            }
        }
        return null;
    }

    // Para bookmarks donde TODO el parrafo es el marcador (ej: "Alumno: Alumno" completo,
    // o las fechas de semana/dia): se borran todos los runs del parrafo y se pone uno solo
    // con el texto nuevo. Se aplica a TODAS las coincidencias encontradas (puede haber 2)
    private void reemplazarParrafoCompleto(XWPFDocument documento, String bookmarkName, String textoNuevo) {
        List<XWPFParagraph> parrafos = parrafosPorBookmark(documento, bookmarkName);
        for (XWPFParagraph parrafo : parrafos) {
            List<XWPFRun> runs = parrafo.getRuns();
            if (runs.isEmpty()) {
                parrafo.createRun().setText(textoNuevo);
                continue;
            }
            runs.get(0).setText(textoNuevo, 0);
            for (int i = runs.size() - 1; i >= 1; i--) {
                parrafo.removeRun(i);
            }
        }
    }

    // Para bookmarks donde el marcador es solo UNA PARTE del parrafo (ej: "Curso: " queda fijo,
    // solo "Curso" -el valor- se reemplaza): buscamos la secuencia de runs cuyo texto concatenado
    // coincide exactamente con el texto original conocido, y reemplazamos solo esos runs.
    // Se aplica a TODAS las coincidencias encontradas (puede haber 2, por los cuadros de texto duplicados)
    private void reemplazarDentroDeParrafo(XWPFDocument documento, String bookmarkName, String textoOriginal, String textoNuevo) {
        List<XWPFParagraph> parrafos = parrafosPorBookmark(documento, bookmarkName);
        for (XWPFParagraph parrafo : parrafos) {
            List<XWPFRun> runs = parrafo.getRuns();
            for (int i = 0; i < runs.size(); i++) {
                StringBuilder acumulado = new StringBuilder();
                boolean reemplazado = false;
                for (int j = i; j < runs.size(); j++) {
                    String textoRun = runs.get(j).getText(0);
                    acumulado.append(textoRun == null ? "" : textoRun);

                    if (acumulado.toString().equals(textoOriginal)) {
                        runs.get(i).setText(textoNuevo, 0);
                        for (int k = j; k > i; k--) {
                            parrafo.removeRun(k);
                        }
                        reemplazado = true;
                        break;
                    }
                    if (acumulado.length() > textoOriginal.length()) break;
                }
                if (reemplazado) break;
            }
        }
    }

    // Borra TODO el bloque de semanas que el alumno no completo de una sola vez (desde la
    // primera semana sobrante hasta CONCLUSIONES), en vez de semana por semana -- borrar de
    // a una traia problemas porque al buscar el limite de una semana usando el bookmark de
    // la semana siguiente, esa siguiente semana ya podia estar borrada del paso anterior
    private void eliminarSemanasSobrantes(XWPFDocument documento, int cantidadPlanillas) {
        if (cantidadPlanillas >= 6) return;

        List<XWPFParagraph> parrafosInicio = parrafosPorBookmark(documento, ORDINAL_SEMANA[cantidadPlanillas] + "_semana_fecha");
        if (parrafosInicio.isEmpty()) return;
        XWPFParagraph parrafoInicio = parrafosInicio.get(0);
        XWPFParagraph parrafoFin = buscarParrafoConTexto(documento, "CONCLUSIONES");

        // ojo: no usamos elementos.indexOf(...) porque los parrafos encontrados por
        // obtenerTodosLosParrafos() son instancias de XWPFParagraph reconstruidas a mano
        // (necesario para llegar a los que estan dentro de cuadros de texto), y no son el
        // mismo objeto Java que POI usa internamente en getBodyElements() -- comparamos el
        // XML subyacente (CTP) en vez de la identidad del objeto envoltorio
        int posInicio = encontrarPosicionEnBody(documento, parrafoInicio);
        int posFin = parrafoFin != null ? encontrarPosicionEnBody(documento, parrafoFin) : documento.getBodyElements().size();

        if (posInicio == -1) return;

        // de atras hacia adelante, para no invalidar los indices ya calculados al ir borrando
        for (int i = posFin - 1; i >= posInicio; i--) {
            documento.removeBodyElement(i);
        }
    }

    private int encontrarPosicionEnBody(XWPFDocument documento, XWPFParagraph objetivo) {
        List<?> elementos = documento.getBodyElements();
        for (int i = 0; i < elementos.size(); i++) {
            Object elemento = elementos.get(i);
            if (elemento instanceof XWPFParagraph && ((XWPFParagraph) elemento).getCTP() == objetivo.getCTP()) {
                return i;
            }
        }
        return -1;
    }
}

