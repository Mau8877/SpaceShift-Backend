package com.sw.api.modules.reporte.service;

import com.sw.api.modules.reporte.dto.ReporteContratoRow;
import com.sw.api.modules.reporte.dto.ReportePublicacionRow;
import com.sw.api.modules.reporte.dto.ReporteResumenDTO;
import com.sw.api.modules.reporte.dto.ReporteUsuarioRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelGeneratorService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS_USUARIOS = {
            "ID", "Correo", "Nombre", "Apellido", "Teléfono",
            "Rol", "Tipo Perfil", "Activo", "En Línea",
            "Fecha Registro", "Última Conexión", "Publicaciones"
    };

    private static final String[] HEADERS_PUBLICACIONES = {
            "ID", "Título", "Tipo Transacción", "Precio", "Moneda",
            "Estado", "Fecha Publicación", "Tipo Inmueble",
            "Área Terreno", "Área Construida", "Habitaciones", "Baños", "Garajes",
            "Ciudad", "Zona / Barrios", "Dirección",
            "Correo Publicador", "Nombre Publicador", "Apellido Publicador"
    };

    private static final String[] HEADERS_CONTRATOS = {
            "ID", "Tipo Contrato", "Estado", "Monto Acordado", "Moneda",
            "Fecha Inicio", "Fecha Fin", "Noches", "Huéspedes",
            "Fecha Creación", "Correo Propietario", "Nombre Propietario",
            "Correo Cliente", "Nombre Cliente",
            "Publicación", "Tipo Inmueble", "Ciudad"
    };

    // -------------------------------------------------------------------------
    // USUARIOS
    // -------------------------------------------------------------------------

    public byte[] generarExcelUsuarios(List<ReporteUsuarioRow> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Usuarios");
            CellStyle headerStyle = headerStyle(wb);
            CellStyle altStyle    = altRowStyle(wb);

            writeHeader(sheet, headerStyle, HEADERS_USUARIOS);

            int rowIdx = 1;
            for (ReporteUsuarioRow r : data) {
                Row row = sheet.createRow(rowIdx);
                CellStyle s = (rowIdx % 2 == 0) ? altStyle : null;
                setStr(row, 0,  str(r.id()), s);
                setStr(row, 1,  str(r.correo()), s);
                setStr(row, 2,  str(r.nombre()), s);
                setStr(row, 3,  str(r.apellido()), s);
                setStr(row, 4,  str(r.telefono()), s);
                setStr(row, 5,  str(r.rol()), s);
                setStr(row, 6,  str(r.tipoPerfil()), s);
                setStr(row, 7,  r.activo() ? "Sí" : "No", s);
                setStr(row, 8,  r.enLinea() ? "Sí" : "No", s);
                setStr(row, 9,  r.fechaRegistro()   != null ? r.fechaRegistro().format(DT_FMT)   : "", s);
                setStr(row, 10, r.ultimaConexion()  != null ? r.ultimaConexion().format(DT_FMT)  : "", s);
                setNum(row, 11, r.totalPublicaciones(), s);
                rowIdx++;
            }

            autoSize(sheet, HEADERS_USUARIOS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel de usuarios", e);
        }
    }

    // -------------------------------------------------------------------------
    // PUBLICACIONES
    // -------------------------------------------------------------------------

    public byte[] generarExcelPublicaciones(List<ReportePublicacionRow> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Publicaciones");
            CellStyle headerStyle = headerStyle(wb);
            CellStyle altStyle    = altRowStyle(wb);

            writeHeader(sheet, headerStyle, HEADERS_PUBLICACIONES);

            int rowIdx = 1;
            for (ReportePublicacionRow r : data) {
                Row row = sheet.createRow(rowIdx);
                CellStyle s = (rowIdx % 2 == 0) ? altStyle : null;
                setStr(row, 0,  str(r.id()), s);
                setStr(row, 1,  str(r.titulo()), s);
                setStr(row, 2,  str(r.tipoTransaccion()), s);
                setNum(row, 3,  r.precio(), s);
                setStr(row, 4,  str(r.moneda()), s);
                setStr(row, 5,  str(r.estadoPublicacion()), s);
                setStr(row, 6,  r.fechaPublicacion() != null ? r.fechaPublicacion().format(DT_FMT) : "", s);
                setStr(row, 7,  str(r.tipoInmueble()), s);
                setNum(row, 8,  r.areaTerreno(), s);
                setNum(row, 9,  r.areaConstruida(), s);
                setNum(row, 10, r.habitaciones(), s);
                setNum(row, 11, r.banos(), s);
                setNum(row, 12, r.garajes(), s);
                setStr(row, 13, str(r.ciudad()), s);
                setStr(row, 14, str(r.zonaBarrios()), s);
                setStr(row, 15, str(r.direccionExacta()), s);
                setStr(row, 16, str(r.correoPublicador()), s);
                setStr(row, 17, str(r.nombrePublicador()), s);
                setStr(row, 18, str(r.apellidoPublicador()), s);
                rowIdx++;
            }

            autoSize(sheet, HEADERS_PUBLICACIONES.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel de publicaciones", e);
        }
    }

    // -------------------------------------------------------------------------
    // CONTRATOS
    // -------------------------------------------------------------------------

    public byte[] generarExcelContratos(List<ReporteContratoRow> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Contratos");
            CellStyle headerStyle = headerStyle(wb);
            CellStyle altStyle    = altRowStyle(wb);

            writeHeader(sheet, headerStyle, HEADERS_CONTRATOS);

            int rowIdx = 1;
            for (ReporteContratoRow r : data) {
                Row row = sheet.createRow(rowIdx);
                CellStyle s = (rowIdx % 2 == 0) ? altStyle : null;
                setStr(row, 0,  str(r.id()), s);
                setStr(row, 1,  str(r.tipoContrato()), s);
                setStr(row, 2,  str(r.estadoContrato()), s);
                setNum(row, 3,  r.montoAcordado(), s);
                setStr(row, 4,  str(r.moneda()), s);
                setStr(row, 5,  r.fechaInicio()  != null ? r.fechaInicio().format(D_FMT)  : "", s);
                setStr(row, 6,  r.fechaFin()     != null ? r.fechaFin().format(D_FMT)     : "", s);
                setNum(row, 7,  r.noches(), s);
                setNum(row, 8,  r.cantidadHuespedes(), s);
                setStr(row, 9,  r.fechaCreacion() != null ? r.fechaCreacion().format(DT_FMT) : "", s);
                setStr(row, 10, str(r.correosPropietario()), s);
                setStr(row, 11, str(r.nombrePropietario()), s);
                setStr(row, 12, str(r.correoCliente()), s);
                setStr(row, 13, str(r.nombreCliente()), s);
                setStr(row, 14, str(r.tituloPublicacion()), s);
                setStr(row, 15, str(r.tipoInmueble()), s);
                setStr(row, 16, str(r.ciudadInmueble()), s);
                rowIdx++;
            }

            autoSize(sheet, HEADERS_CONTRATOS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel de contratos", e);
        }
    }

    // -------------------------------------------------------------------------
    // RESUMEN
    // -------------------------------------------------------------------------

    public byte[] generarExcelResumen(ReporteResumenDTO r) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Resumen");
            CellStyle sectionStyle = sectionStyle(wb);
            CellStyle labelStyle   = labelStyle(wb);
            CellStyle valueStyle   = valueStyle(wb);

            int row = 0;

            row = writeSection(sheet, row, "USUARIOS", sectionStyle);
            row = writeKV(sheet, row, "Total usuarios",            r.totalUsuarios(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Usuarios activos",          r.usuariosActivos(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Usuarios inactivos",        r.usuariosInactivos(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Usuarios con rol USER",     r.usuariosRoleUser(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Usuarios con rol ADMIN",    r.usuariosRoleAdmin(), labelStyle, valueStyle);
            row++;

            row = writeSection(sheet, row, "PUBLICACIONES", sectionStyle);
            row = writeKV(sheet, row, "Total publicaciones",       r.totalPublicaciones(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Disponibles",               r.publicacionesDisponibles(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "En venta",                  r.publicacionesEnVenta(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "En alquiler",               r.publicacionesEnAlquiler(), labelStyle, valueStyle);
            row++;

            row = writeSection(sheet, row, "CONTRATOS", sectionStyle);
            row = writeKV(sheet, row, "Total contratos",           r.totalContratos(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Activos",                   r.contratosActivos(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Cerrados",                  r.contratosCerrados(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Cancelados",                r.contratosCancelados(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Tipo compraventa",          r.contratosCompraventa(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Tipo alquiler",             r.contratosAlquiler(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Tipo hospedaje",            r.contratosHospedaje(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Monto total contratos",     r.montoTotalContratos(), labelStyle, valueStyle);
            row = writeKV(sheet, row, "Monto contratos activos",   r.montoContratosActivos(), labelStyle, valueStyle);

            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 5000);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel de resumen", e);
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS PRIVADOS
    // -------------------------------------------------------------------------

    private void writeHeader(Sheet sheet, CellStyle style, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private int writeSection(Sheet sheet, int rowIdx, String title, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
        return rowIdx + 1;
    }

    private int writeKV(Sheet sheet, int rowIdx, String label, Object value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        if (value instanceof Number n) {
            valueCell.setCellValue(n.doubleValue());
        } else {
            valueCell.setCellValue(value != null ? value.toString() : "");
        }
        valueCell.setCellStyle(valueStyle);
        return rowIdx + 1;
    }

    private void setStr(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void setNum(Row row, int col, Number value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) cell.setCellValue(value.doubleValue());
        if (style != null) cell.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            int current = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(current + 512, 20000));
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    // ---- Estilos ----

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle altRowStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle sectionStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle labelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle valueStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
