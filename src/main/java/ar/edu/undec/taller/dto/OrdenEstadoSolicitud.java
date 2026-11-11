package ar.edu.undec.taller.dto;

import ar.edu.undec.taller.model.EstadoOrden;
import jakarta.validation.constraints.NotNull;

public record OrdenEstadoSolicitud(
		@NotNull(message = "El estado es obligatorio") EstadoOrden estado) {
}
