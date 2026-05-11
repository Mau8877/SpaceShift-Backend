package com.sw.api.modules.reporte.controller;

import com.sw.api.modules.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // -------------------------------------------------------------------------
    // USUARIOS
    // -------------------------------------------------------------------------

    @GetMapping("/usuarios/excel")
    public ResponseEntity<byte[]> usuariosExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarExcelUsuarios(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios-" + today() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MIME))
                .body(bytes);
    }

    @GetMapping("/usuarios/pdf")
    public ResponseEntity<byte[]> usuariosPdf(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarPdfUsuarios(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-usuarios-" + today() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // -------------------------------------------------------------------------
    // PUBLICACIONES
    // -------------------------------------------------------------------------

    @GetMapping("/publicaciones/excel")
    public ResponseEntity<byte[]> publicacionesExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarExcelPublicaciones(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-publicaciones-" + today() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MIME))
                .body(bytes);
    }

    @GetMapping("/publicaciones/pdf")
    public ResponseEntity<byte[]> publicacionesPdf(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarPdfPublicaciones(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-publicaciones-" + today() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // -------------------------------------------------------------------------
    // CONTRATOS
    // -------------------------------------------------------------------------

    @GetMapping("/contratos/excel")
    public ResponseEntity<byte[]> contratosExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarExcelContratos(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-contratos-" + today() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MIME))
                .body(bytes);
    }

    @GetMapping("/contratos/pdf")
    public ResponseEntity<byte[]> contratosPdf(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        byte[] bytes = reporteService.generarPdfContratos(parseStart(fechaInicio), parseEnd(fechaFin));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-contratos-" + today() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // -------------------------------------------------------------------------
    // RESUMEN
    // -------------------------------------------------------------------------

    @GetMapping("/resumen/excel")
    public ResponseEntity<byte[]> resumenExcel() {
        byte[] bytes = reporteService.generarExcelResumen();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resumen-" + today() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(EXCEL_MIME))
                .body(bytes);
    }

    @GetMapping("/resumen/pdf")
    public ResponseEntity<byte[]> resumenPdf() {
        byte[] bytes = reporteService.generarPdfResumen();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resumen-" + today() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private LocalDateTime parseStart(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw).atStartOfDay();
    }

    private LocalDateTime parseEnd(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw).atTime(23, 59, 59);
    }

    private String today() {
        return LocalDate.now().toString();
    }
}
