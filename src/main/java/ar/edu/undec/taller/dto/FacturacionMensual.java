package ar.edu.undec.taller.dto;

import java.math.BigDecimal;

public record FacturacionMensual(long cantidadOrdenes, BigDecimal totalFacturado) {
}
