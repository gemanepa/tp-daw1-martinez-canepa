package ar.edu.undec.taller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrdenSolicitud(
		@NotNull(message = "El vehículo es obligatorio") Long vehiculoId,
		@NotBlank(message = "La descripción es obligatoria") String descripcion) {
}
