package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReporteServicioResponse(
    int totalServicios,
    int activos,
    int inactivos,
    String categoriaMasUsada,
    String servicioMasSolicitado,
    BigDecimal ingresosMes,
    BigDecimal ingresosAnio,
    List<CategoriaCount> categorias,
    List<ServicioSolicitadoResponse> topServicios,
    List<IngresoPorServicio> ingresosPorServicio,
    List<TendenciaMensual> tendenciaMensual
) {
    public record CategoriaCount(String categoria, int cantidad) {}
    public record IngresoPorServicio(String nombre, BigDecimal precio, long cantidad, BigDecimal totalGenerado) {}
    public record TendenciaMensual(String mes, int anio, long cantidad) {}
}
