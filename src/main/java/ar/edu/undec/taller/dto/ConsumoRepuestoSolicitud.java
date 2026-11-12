package ar.edu.undec.taller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumoRepuestoSolicitud(
		@NotNull(message = "El repuesto es obligatorio") Long repuestoId,
		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor que cero") Integer cantidad) {
}
