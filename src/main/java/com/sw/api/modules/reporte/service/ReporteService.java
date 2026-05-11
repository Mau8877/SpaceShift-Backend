package com.sw.api.modules.reporte.service;

import com.sw.api.modules.reporte.dto.ReporteContratoRow;
import com.sw.api.modules.reporte.dto.ReportePublicacionRow;
import com.sw.api.modules.reporte.dto.ReporteResumenDTO;
import com.sw.api.modules.reporte.dto.ReporteUsuarioRow;
import com.sw.api.modules.reporte.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final ExcelGeneratorService excelGenerator;
    private final PdfGeneratorService pdfGenerator;

    // -------------------------------------------------------------------------
    // DATOS
    // -------------------------------------------------------------------------

    public List<ReporteUsuarioRow> obtenerDatosUsuarios(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return reporteRepository.findReporteUsuarios(fechaInicio, fechaFin).stream()
                .map(p -> new ReporteUsuarioRow(
                        p.getId(),
                        p.getCorreo(),
                        p.getNombre(),
                        p.getApellido(),
                        p.getTelefono(),
                        p.getRol(),
                        p.getTipoPerfil(),
                        Boolean.TRUE.equals(p.getActivo()),
                        Boolean.TRUE.equals(p.getEnLinea()),
                        p.getFechaRegistro(),
                        p.getUltimaConexion(),
                        p.getTotalPublicaciones() != null ? p.getTotalPublicaciones() : 0L
                )).toList();
    }

    public List<ReportePublicacionRow> obtenerDatosPublicaciones(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return reporteRepository.findReportePublicaciones(fechaInicio, fechaFin).stream()
                .map(p -> new ReportePublicacionRow(
                        p.getId(),
                        p.getTitulo(),
                        p.getTipoTransaccion(),
                        p.getPrecio(),
                        p.getMoneda(),
                        p.getEstadoPublicacion(),
                        p.getFechaPublicacion(),
                        p.getTipoInmueble(),
                        p.getAreaTerreno(),
                        p.getAreaConstruida(),
                        p.getHabitaciones(),
                        p.getBanos(),
                        p.getGarajes(),
                        p.getCiudad(),
                        p.getZonaBarrios(),
                        p.getDireccionExacta(),
                        p.getCorreoPublicador(),
                        p.getNombrePublicador(),
                        p.getApellidoPublicador()
                )).toList();
    }

    public List<ReporteContratoRow> obtenerDatosContratos(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return reporteRepository.findReporteContratos(fechaInicio, fechaFin).stream()
                .map(p -> new ReporteContratoRow(
                        p.getId(),
                        p.getTipoContrato(),
                        p.getEstadoContrato(),
                        p.getMontoAcordado(),
                        p.getMoneda(),
                        p.getFechaInicio(),
                        p.getFechaFin(),
                        p.getNoches(),
                        p.getCantidadHuespedes(),
                        p.getFechaCreacion(),
                        p.getCorreosPropietario(),
                        p.getNombrePropietario(),
                        p.getCorreoCliente(),
                        p.getNombreCliente(),
                        p.getTituloPublicacion(),
                        p.getTipoInmueble(),
                        p.getCiudadInmueble()
                )).toList();
    }

    public ReporteResumenDTO obtenerResumen() {
        return new ReporteResumenDTO(
                reporteRepository.countTotalUsuarios(),
                reporteRepository.countUsuariosActivos(),
                reporteRepository.countUsuariosInactivos(),
                reporteRepository.countRoleUser(),
                reporteRepository.countRoleAdmin(),
                reporteRepository.countTotalPublicaciones(),
                reporteRepository.countPublicacionesDisponibles(),
                reporteRepository.countPublicacionesVenta(),
                reporteRepository.countPublicacionesAlquiler(),
                reporteRepository.countTotalContratos(),
                reporteRepository.countContratosActivos(),
                reporteRepository.countContratosCerrados(),
                reporteRepository.countContratosCancelados(),
                reporteRepository.countContratosCompraventa(),
                reporteRepository.countContratosAlquiler(),
                reporteRepository.countContratosHospedaje(),
                reporteRepository.sumMontoTotal(),
                reporteRepository.sumMontoActivos()
        );
    }

    // -------------------------------------------------------------------------
    // EXCEL
    // -------------------------------------------------------------------------

    public byte[] generarExcelUsuarios(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return excelGenerator.generarExcelUsuarios(obtenerDatosUsuarios(fechaInicio, fechaFin));
    }

    public byte[] generarExcelPublicaciones(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return excelGenerator.generarExcelPublicaciones(obtenerDatosPublicaciones(fechaInicio, fechaFin));
    }

    public byte[] generarExcelContratos(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return excelGenerator.generarExcelContratos(obtenerDatosContratos(fechaInicio, fechaFin));
    }

    public byte[] generarExcelResumen() {
        return excelGenerator.generarExcelResumen(obtenerResumen());
    }

    // -------------------------------------------------------------------------
    // PDF
    // -------------------------------------------------------------------------

    public byte[] generarPdfUsuarios(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pdfGenerator.generarPdfUsuarios(obtenerDatosUsuarios(fechaInicio, fechaFin));
    }

    public byte[] generarPdfPublicaciones(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pdfGenerator.generarPdfPublicaciones(obtenerDatosPublicaciones(fechaInicio, fechaFin));
    }

    public byte[] generarPdfContratos(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pdfGenerator.generarPdfContratos(obtenerDatosContratos(fechaInicio, fechaFin));
    }

    public byte[] generarPdfResumen() {
        return pdfGenerator.generarPdfResumen(obtenerResumen());
    }
}
