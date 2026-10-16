package com.EscuelaEmpresa.gestor_pasantes.service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Alumno;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanal;
import com.EscuelaEmpresa.gestor_pasantes.entity.PlanillaSemanalDetalle;
import com.EscuelaEmpresa.gestor_pasantes.repository.PlanillaSemanalDetalleRepository;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHeight;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVerticalJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
    private final PlantillaService plantillaService;

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

    public InformePasantiaService(PlanillaSemanalDetalleRepository detalleRepository, PlantillaService plantillaService) {
        this.detalleRepository = detalleRepository;
        this.plantillaService = plantillaService;
    }

    /**
     * @param planillasOrdenadas las planillas del alumno, ordenadas de la semana MAS ANTIGUA a la MAS NUEVA
     */
    public XWPFDocument generarInforme(Alumno alumno, List<PlanillaSemanal> planillasOrdenadas) throws IOException {

        XWPFDocument documento;
        try (InputStream entrada = plantillaService.abrir("Informe_Pasantia_Plantilla.docx")) {
            documento = new XWPFDocument(entrada);
        }

        completarEncabezado(documento, alumno, planillasOrdenadas);
        completarSemanas(documento, planillasOrdenadas);
        agregarCuadroFinal(documento, alumno);

        return documento;
    }

    private void completarEncabezado(XWPFDocument documento, Alumno alumno, List<PlanillaSemanal> planillas) {

        boolean esMujer = "Femenino".equalsIgnoreCase(alumno.getSexo());
        String nombreCompleto = alumno.getNombres() + " " + alumno.getApellidos();
        reemplazarParrafoCompleto(documento, "alumno", (esMujer ? "Alumna: " : "Alumno: ") + nombreCompleto);

        reemplazarDentroDeParrafo(documento, "curso", "Curso", alumno.getCurso());
        reemplazarDentroDeParrafo(documento, "turno", "Turno", alumno.getSeccion() != null ? alumno.getSeccion() : "");
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

                if (detalleDelDia != null) {
                    String fecha = detalleDelDia.getFecha().format(FORMATO_FECHA_CORTA);
                    String textoFinal = NOMBRE_DIA_TEXTO[dia] + " " + fecha + ": " + detalleDelDia.getDescripcion();
                    reemplazarParrafoCompleto(documento, bookmarkDia, textoFinal);
                } else {
                    // Sin actividad registrada ese día: se saca la línea entera
                    // del informe en vez de dejarla con un "Sin actividades
                    // registradas" -- si el alumno solo trabajó el lunes de esa
                    // semana, el resto de los días ni aparece.
                    eliminarParrafoPorBookmark(documento, bookmarkDia);
                }
            }
        }

        eliminarSemanasSobrantes(documento, cantidad);
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

    // Para los días sin actividad: se saca el párrafo entero del cuerpo del
    // documento, no solo se le cambia el texto. Mismo mecanismo que
    // eliminarSemanasSobrantes (buscar la posición real en getBodyElements()
    // y sacarla de ahí), pero acá es un párrafo suelto en vez de un rango.
    private void eliminarParrafoPorBookmark(XWPFDocument documento, String bookmarkName) {
        XWPFParagraph parrafo = buscarParrafoPorBookmark(documento, bookmarkName);
        if (parrafo == null) return;

        List<IBodyElement> elementos = documento.getBodyElements();
        int indice = elementos.indexOf(parrafo);
        if (indice != -1) {
            documento.removeBodyElement(indice);
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

    // ---------- Cuadro final (hoja de firmas) ----------

    // El Director ya aparece con este mismo nombre, fijo, en las plantillas PDF
    // (Contrato, Autorizacion): no hay todavia un lugar unico de donde sacarlo,
    // asi que si cambia hay que actualizar ese texto ahi y tambien aca.
    private static final String NOMBRE_DIRECTOR = "Alcides R. Hosffmeister V.";

    private static final int ANCHO_COLUMNA_ETIQUETA = 2270; // dxa (~4 cm)
    private static final int ANCHO_COLUMNA_CONTENIDO = 6518; // dxa (~11.5 cm)
    private static final int[] ALTURAS_FILA = {961, 958, 958, 958, 1105, 997, 644}; // dxa

    // Hoja que se agrega al final del informe, en pagina aparte: es la version
    // Word del cuadro de firmas "Presentado por / Aprobado por / Lugar y
    // Fecha" (antes era un Excel que se imprimia aparte y se completaba a
    // mano encima). No es un bookmark de la plantilla: se arma la tabla de
    // cero porque ese cuadro no formaba parte del .docx original. El unico
    // dato que se completa solo es el nombre del pasante (lo pidio Julio
    // puntualmente); el resto -- tutor, firmas, lugar y fecha -- queda en
    // blanco para completarse a mano, igual que en el cuadro original.
    private void agregarCuadroFinal(XWPFDocument documento, Alumno alumno) {
        XWPFParagraph saltoPagina = documento.createParagraph();
        saltoPagina.createRun().addBreak(BreakType.PAGE);

        XWPFTable tabla = documento.createTable(7, 2);
        configurarTablaCuadroFinal(tabla);

        String nombreCompleto = alumno.getNombres() + " " + alumno.getApellidos();

        escribirCeldaEtiqueta(tabla, 0, "NOMBRE Y APELLIDO DEL PASANTE", nombreCompleto);
        escribirCeldaEtiqueta(tabla, 1, "Firma", null);
        escribirCeldaEtiqueta(tabla, 2, "NOMBRE Y APELLIDO DEL TUTOR DE PASANTIA", null);
        escribirCeldaEtiqueta(tabla, 3, "Firma:", null);
        escribirCeldaEtiqueta(tabla, 4, "NOMBRE Y APELLIDO DEL DIRECTOR", NOMBRE_DIRECTOR);
        escribirCeldaEtiqueta(tabla, 5, "Firma:", null);

        fusionarColumnaEtiqueta(tabla, 0, 1, "Presentado por:");
        fusionarColumnaEtiqueta(tabla, 2, 5, "Aprobado por:");

        XWPFTableCell celdaLugar = tabla.getRow(6).getCell(0);
        celdaLugar.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        aplicarEstiloCuadroFinal(celdaLugar.getParagraphs().get(0).createRun(), "Lugar y Fecha:", false);
        centrarVerticalmente(celdaLugar);

        XWPFTableCell celdaFecha = tabla.getRow(6).getCell(1);
        aplicarEstiloCuadroFinal(celdaFecha.getParagraphs().get(0).createRun(),
                "Asunción,                 de noviembre del año", false);
    }

    // Cada celda de la columna de contenido tiene la misma forma que en el
    // cuadro original: una etiqueta en mayuscula arriba y, si ya hay un dato
    // (el nombre del pasante, o el del Director que ya viene fijo), una
    // segunda linea centrada y en negrita debajo. Sin dato, la celda queda
    // con la etiqueta nomas y el resto en blanco, para completarse a mano.
    private void escribirCeldaEtiqueta(XWPFTable tabla, int fila, String etiqueta, String valor) {
        XWPFTableCell celda = tabla.getRow(fila).getCell(1);
        aplicarEstiloCuadroFinal(celda.getParagraphs().get(0).createRun(), etiqueta, false);

        if (valor != null && !valor.isBlank()) {
            XWPFParagraph parrafoValor = celda.addParagraph();
            parrafoValor.setAlignment(ParagraphAlignment.CENTER);
            parrafoValor.setSpacingBefore(150);
            aplicarEstiloCuadroFinal(parrafoValor.createRun(), valor, true);
        }
    }

    private void aplicarEstiloCuadroFinal(XWPFRun run, String texto, boolean negrita) {
        run.setText(texto);
        run.setFontFamily("Arial");
        run.setFontSize(10);
        run.setBold(negrita);
    }

    // "Presentado por:"/"Aprobado por:" ocupan, fusionadas verticalmente, el
    // mismo rango de filas que las etiquetas de la columna de contenido a su
    // derecha (ver agregarCuadroFinal) -- filaInicio..filaFin inclusive.
    private void fusionarColumnaEtiqueta(XWPFTable tabla, int filaInicio, int filaFin, String etiqueta) {
        XWPFTableCell celdaInicio = tabla.getRow(filaInicio).getCell(0);
        celdaInicio.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        aplicarEstiloCuadroFinal(celdaInicio.getParagraphs().get(0).createRun(), etiqueta, false);
        centrarVerticalmente(celdaInicio);
        marcarVMerge(celdaInicio, STMerge.RESTART);

        for (int fila = filaInicio + 1; fila <= filaFin; fila++) {
            marcarVMerge(tabla.getRow(fila).getCell(0), STMerge.CONTINUE);
        }
    }

    private void centrarVerticalmente(XWPFTableCell celda) {
        CTTcPr tcPr = celda.getCTTc().isSetTcPr() ? celda.getCTTc().getTcPr() : celda.getCTTc().addNewTcPr();
        CTVerticalJc vAlign = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr.addNewVAlign();
        vAlign.setVal(STVerticalJc.CENTER);
    }

    private void marcarVMerge(XWPFTableCell celda, STMerge.Enum tipo) {
        CTTc ctTc = celda.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTVMerge vMerge = tcPr.isSetVMerge() ? tcPr.getVMerge() : tcPr.addNewVMerge();
        vMerge.setVal(tipo);
    }

    // Ancho fijo de columnas (en vez de dejar que Word las autoajuste al
    // contenido), bordes -- mas grueso en el perimetro, fino entre celdas,
    // igual que en el cuadro original -- y una altura minima por fila para
    // que no quede todo amontonado.
    private void configurarTablaCuadroFinal(XWPFTable tabla) {
        CTTbl tablaXml = tabla.getCTTbl();
        // tblPr y tblGrid son obligatorios en el esquema (CT_Tbl los exige con
        // minOccurs=1, a diferencia de sus propios hijos como tblW/tblBorders
        // que sí son opcionales): por eso acá no hay isSetTblPr/isSetTblGrid
        // -- XWPFTable.createTable ya los crea siempre, así que se usan
        // directo con getTblPr()/getTblGrid().
        CTTblPr tblPr = tablaXml.getTblPr();

        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(ANCHO_COLUMNA_ETIQUETA + ANCHO_COLUMNA_CONTENIDO));

        (tblPr.isSetTblLayout() ? tblPr.getTblLayout() : tblPr.addNewTblLayout()).setType(STTblLayoutType.FIXED);

        CTTblBorders bordes = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        fijarBorde(bordes.isSetTop() ? bordes.getTop() : bordes.addNewTop(), 16);
        fijarBorde(bordes.isSetBottom() ? bordes.getBottom() : bordes.addNewBottom(), 16);
        fijarBorde(bordes.isSetLeft() ? bordes.getLeft() : bordes.addNewLeft(), 16);
        fijarBorde(bordes.isSetRight() ? bordes.getRight() : bordes.addNewRight(), 16);
        fijarBorde(bordes.isSetInsideH() ? bordes.getInsideH() : bordes.addNewInsideH(), 4);
        fijarBorde(bordes.isSetInsideV() ? bordes.getInsideV() : bordes.addNewInsideV(), 4);

        CTTblGrid grilla = tablaXml.getTblGrid();
        while (grilla.sizeOfGridColArray() > 0) {
            grilla.removeGridCol(0);
        }
        CTTblGridCol columnaEtiqueta = grilla.addNewGridCol();
        columnaEtiqueta.setW(BigInteger.valueOf(ANCHO_COLUMNA_ETIQUETA));
        CTTblGridCol columnaContenido = grilla.addNewGridCol();
        columnaContenido.setW(BigInteger.valueOf(ANCHO_COLUMNA_CONTENIDO));

        for (int i = 0; i < tabla.getNumberOfRows(); i++) {
            XWPFTableRow fila = tabla.getRow(i);
            establecerAnchoCelda(fila.getCell(0), ANCHO_COLUMNA_ETIQUETA);
            establecerAnchoCelda(fila.getCell(1), ANCHO_COLUMNA_CONTENIDO);
            establecerAlturaFila(fila, ALTURAS_FILA[i]);
        }
    }

    private void fijarBorde(CTBorder borde, int grosor) {
        borde.setVal(STBorder.SINGLE);
        borde.setSz(BigInteger.valueOf(grosor));
        borde.setColor("000000");
    }

    private void establecerAnchoCelda(XWPFTableCell celda, int anchoDxa) {
        CTTc ctTc = celda.getCTTc();
        CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(BigInteger.valueOf(anchoDxa));
    }

    private void establecerAlturaFila(XWPFTableRow fila, int alturaDxa) {
        CTTrPr trPr = fila.getCtRow().isSetTrPr() ? fila.getCtRow().getTrPr() : fila.getCtRow().addNewTrPr();
        // trHeight es un elemento repetible a nivel de esquema (por eso es
        // sizeOfTrHeightArray/getTrHeightArray/addNewTrHeight y no
        // isSetTrHeight/getTrHeight como el resto de las propiedades de acá),
        // aunque en la práctica una fila nunca tiene más de uno.
        CTHeight altura = trPr.sizeOfTrHeightArray() == 0 ? trPr.addNewTrHeight() : trPr.getTrHeightArray(0);
        altura.setVal(BigInteger.valueOf(alturaDxa));
        altura.setHRule(STHeightRule.AT_LEAST);
    }
}
