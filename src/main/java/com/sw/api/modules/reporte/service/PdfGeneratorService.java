package com.sw.api.modules.reporte.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.sw.api.modules.reporte.dto.ReporteContratoRow;
import com.sw.api.modules.reporte.dto.ReportePublicacionRow;
import com.sw.api.modules.reporte.dto.ReporteResumenDTO;
import com.sw.api.modules.reporte.dto.ReporteUsuarioRow;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GEN_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Color NAVY      = new Color(0, 51, 102);
    private static final Color ALT_BLUE  = new Color(220, 230, 241);
    private static final Color WHITE     = Color.WHITE;
    private static final Color GRAY_TEXT = new Color(100, 100, 100);

    private static final Font TITLE_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, WHITE);
    private static final Font HEADER_FONT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE);
    private static final Font DATA_FONT    = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
    private static final Font LABEL_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font VALUE_FONT   = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font GEN_FONT     = FontFactory.getFont(FontFactory.HELVETICA, 7, GRAY_TEXT);

    // -------------------------------------------------------------------------
    // USUARIOS
    // -------------------------------------------------------------------------

    public byte[] generarPdfUsuarios(List<ReporteUsuarioRow> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 35, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "Reporte de Usuarios — SpaceShift");
            addTimestamp(doc);

            float[] widths = {3f, 5f, 3f, 3f, 3f, 3f, 3f, 2f, 2f, 4f, 4f, 2f};
            PdfPTable table = new PdfPTable(widths.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setWidths(widths);

            String[] headers = {"ID", "Correo", "Nombre", "Apellido", "Teléfono",
                    "Rol", "Tipo Perfil", "Activo", "En Línea",
                    "Fecha Registro", "Última Conexión", "Pub."};
            for (String h : headers) addHeaderCell(table, h);

            boolean alt = false;
            for (ReporteUsuarioRow r : data) {
                Color bg = alt ? ALT_BLUE : WHITE;
                addDataCell(table, str(r.id()),                                              bg);
                addDataCell(table, str(r.correo()),                                          bg);
                addDataCell(table, str(r.nombre()),                                          bg);
                addDataCell(table, str(r.apellido()),                                        bg);
                addDataCell(table, str(r.telefono()),                                        bg);
                addDataCell(table, str(r.rol()),                                             bg);
                addDataCell(table, str(r.tipoPerfil()),                                      bg);
                addDataCell(table, r.activo()  ? "Sí" : "No",                               bg);
                addDataCell(table, r.enLinea() ? "Sí" : "No",                               bg);
                addDataCell(table, r.fechaRegistro()  != null ? r.fechaRegistro().format(DT_FMT)  : "", bg);
                addDataCell(table, r.ultimaConexion() != null ? r.ultimaConexion().format(DT_FMT) : "", bg);
                addDataCell(table, String.valueOf(r.totalPublicaciones()),                   bg);
                alt = !alt;
            }

            doc.add(table);
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de usuarios", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // PUBLICACIONES
    // -------------------------------------------------------------------------

    public byte[] generarPdfPublicaciones(List<ReportePublicacionRow> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 35, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "Reporte de Publicaciones — SpaceShift");
            addTimestamp(doc);

            float[] widths = {2.5f, 4f, 3f, 2.5f, 2f, 3f, 3f, 3f, 2.5f, 2.5f, 2f, 2f, 2f, 3f, 3f, 4f, 4f, 3f, 3f};
            PdfPTable table = new PdfPTable(widths.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setWidths(widths);

            String[] headers = {"ID", "Título", "Tipo Transac.", "Precio", "Moneda",
                    "Estado", "Fecha Public.", "Tipo Inmueble",
                    "Área Terreno", "Área Constr.", "Hab.", "Baños", "Garajes",
                    "Ciudad", "Zona/Barrios", "Dirección",
                    "Correo Pub.", "Nombre Pub.", "Apellido Pub."};
            for (String h : headers) addHeaderCell(table, h);

            boolean alt = false;
            for (ReportePublicacionRow r : data) {
                Color bg = alt ? ALT_BLUE : WHITE;
                addDataCell(table, str(r.id()),                                                                    bg);
                addDataCell(table, str(r.titulo()),                                                                bg);
                addDataCell(table, str(r.tipoTransaccion()),                                                       bg);
                addDataCell(table, r.precio() != null ? r.precio().toPlainString() : "",                           bg);
                addDataCell(table, str(r.moneda()),                                                                bg);
                addDataCell(table, str(r.estadoPublicacion()),                                                     bg);
                addDataCell(table, r.fechaPublicacion() != null ? r.fechaPublicacion().format(DT_FMT) : "",        bg);
                addDataCell(table, str(r.tipoInmueble()),                                                          bg);
                addDataCell(table, r.areaTerreno()   != null ? r.areaTerreno().toPlainString()   : "",             bg);
                addDataCell(table, r.areaConstruida() != null ? r.areaConstruida().toPlainString() : "",           bg);
                addDataCell(table, str(r.habitaciones()),                                                          bg);
                addDataCell(table, str(r.banos()),                                                                 bg);
                addDataCell(table, str(r.garajes()),                                                               bg);
                addDataCell(table, str(r.ciudad()),                                                                bg);
                addDataCell(table, str(r.zonaBarrios()),                                                           bg);
                addDataCell(table, str(r.direccionExacta()),                                                       bg);
                addDataCell(table, str(r.correoPublicador()),                                                      bg);
                addDataCell(table, str(r.nombrePublicador()),                                                      bg);
                addDataCell(table, str(r.apellidoPublicador()),                                                    bg);
                alt = !alt;
            }

            doc.add(table);
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de publicaciones", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // CONTRATOS
    // -------------------------------------------------------------------------

    public byte[] generarPdfContratos(List<ReporteContratoRow> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 25, 25, 35, 25);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "Reporte de Contratos — SpaceShift");
            addTimestamp(doc);

            float[] widths = {2.5f, 3f, 3f, 3f, 2f, 3f, 3f, 2f, 2f, 3.5f, 4f, 3f, 4f, 3f, 4f, 3f, 3f};
            PdfPTable table = new PdfPTable(widths.length);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setWidths(widths);

            String[] headers = {"ID", "Tipo Contrato", "Estado", "Monto", "Moneda",
                    "Fecha Inicio", "Fecha Fin", "Noches", "Huéspedes",
                    "Fecha Creación", "Correo Propietario", "Nombre Prop.",
                    "Correo Cliente", "Nombre Cliente",
                    "Publicación", "Tipo Inmueble", "Ciudad"};
            for (String h : headers) addHeaderCell(table, h);

            boolean alt = false;
            for (ReporteContratoRow r : data) {
                Color bg = alt ? ALT_BLUE : WHITE;
                addDataCell(table, str(r.id()),                                                                    bg);
                addDataCell(table, str(r.tipoContrato()),                                                          bg);
                addDataCell(table, str(r.estadoContrato()),                                                        bg);
                addDataCell(table, r.montoAcordado() != null ? r.montoAcordado().toPlainString() : "",             bg);
                addDataCell(table, str(r.moneda()),                                                                bg);
                addDataCell(table, r.fechaInicio() != null ? r.fechaInicio().format(D_FMT) : "",                   bg);
                addDataCell(table, r.fechaFin()    != null ? r.fechaFin().format(D_FMT)    : "",                   bg);
                addDataCell(table, str(r.noches()),                                                                bg);
                addDataCell(table, str(r.cantidadHuespedes()),                                                     bg);
                addDataCell(table, r.fechaCreacion() != null ? r.fechaCreacion().format(DT_FMT) : "",              bg);
                addDataCell(table, str(r.correosPropietario()),                                                    bg);
                addDataCell(table, str(r.nombrePropietario()),                                                     bg);
                addDataCell(table, str(r.correoCliente()),                                                         bg);
                addDataCell(table, str(r.nombreCliente()),                                                         bg);
                addDataCell(table, str(r.tituloPublicacion()),                                                     bg);
                addDataCell(table, str(r.tipoInmueble()),                                                          bg);
                addDataCell(table, str(r.ciudadInmueble()),                                                        bg);
                alt = !alt;
            }

            doc.add(table);
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de contratos", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // RESUMEN
    // -------------------------------------------------------------------------

    public byte[] generarPdfResumen(ReporteResumenDTO r) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "Resumen General — SpaceShift");
            addTimestamp(doc);

            addSectionTitle(doc, "Usuarios");
            PdfPTable tu = kvTable();
            addKV(tu, "Total usuarios",            str(r.totalUsuarios()));
            addKV(tu, "Usuarios activos",          str(r.usuariosActivos()));
            addKV(tu, "Usuarios inactivos",        str(r.usuariosInactivos()));
            addKV(tu, "Usuarios con rol USER",     str(r.usuariosRoleUser()));
            addKV(tu, "Usuarios con rol ADMIN",    str(r.usuariosRoleAdmin()));
            doc.add(tu);

            addSectionTitle(doc, "Publicaciones");
            PdfPTable tp = kvTable();
            addKV(tp, "Total publicaciones",       str(r.totalPublicaciones()));
            addKV(tp, "Disponibles",               str(r.publicacionesDisponibles()));
            addKV(tp, "En venta",                  str(r.publicacionesEnVenta()));
            addKV(tp, "En alquiler",               str(r.publicacionesEnAlquiler()));
            doc.add(tp);

            addSectionTitle(doc, "Contratos");
            PdfPTable tc = kvTable();
            addKV(tc, "Total contratos",           str(r.totalContratos()));
            addKV(tc, "Activos",                   str(r.contratosActivos()));
            addKV(tc, "Cerrados",                  str(r.contratosCerrados()));
            addKV(tc, "Cancelados",                str(r.contratosCancelados()));
            addKV(tc, "Tipo compraventa",          str(r.contratosCompraventa()));
            addKV(tc, "Tipo alquiler",             str(r.contratosAlquiler()));
            addKV(tc, "Tipo hospedaje",            str(r.contratosHospedaje()));
            addKV(tc, "Monto total contratos",     r.montoTotalContratos() != null ? r.montoTotalContratos().toPlainString() : "0");
            addKV(tc, "Monto contratos activos",   r.montoContratosActivos() != null ? r.montoContratosActivos().toPlainString() : "0");
            doc.add(tc);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de resumen", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    public byte[] generarPdfReciboPago(
            String codigoContrato,
            String nombreCliente,
            String correoCliente,
            String concepto,
            java.math.BigDecimal monto,
            String moneda,
            String stripePaymentId,
            LocalDateTime fechaPago) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "COMPROBANTE DE PAGO — SPACESHIFT");
            addTimestamp(doc);

            PdfPTable table = kvTable();
            addKV(table, "Código de Contrato", codigoContrato);
            addKV(table, "Cliente", nombreCliente);
            addKV(table, "Correo Electrónico", correoCliente);
            addKV(table, "Concepto de Pago", concepto);
            addKV(table, "Monto Acreditado", moneda + " " + monto.toPlainString());
            addKV(table, "ID Transacción Stripe", stripePaymentId != null ? stripePaymentId : "N/A");
            addKV(table, "Fecha de Pago", fechaPago != null ? fechaPago.format(GEN_FMT) : LocalDateTime.now().format(GEN_FMT));
            doc.add(table);

            Paragraph footer = new Paragraph("\n\nGracias por confiar en SpaceShift. Este documento es un comprobante de transacción digital válido.", GEN_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de recibo", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------
    // HELPERS PRIVADOS
    // -------------------------------------------------------------------------

    private void addTitle(Document doc, String text) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(text, TITLE_FONT));
        cell.setBackgroundColor(NAVY);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        banner.addCell(cell);
        doc.add(banner);
    }

    private void addTimestamp(Document doc) throws DocumentException {
        Paragraph p = new Paragraph(
                "Generado el: " + LocalDateTime.now().format(GEN_FMT), GEN_FONT);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(6f);
        doc.add(p);
    }

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, SECTION_FONT);
        p.setSpacingBefore(12f);
        p.setSpacingAfter(4f);
        doc.add(p);
    }

    private PdfPTable kvTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(70);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{4f, 2f});
        table.setSpacingAfter(4f);
        return table;
    }

    private void addKV(PdfPTable table, String label, String value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, LABEL_FONT));
        lc.setBackgroundColor(ALT_BLUE);
        lc.setPadding(5);
        lc.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, VALUE_FONT));
        vc.setPadding(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(vc);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(NAVY);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", DATA_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(3);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
}
