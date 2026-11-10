package ar.edu.undec.taller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VehiculoSolicitud(
		@NotBlank(message = "La patente es obligatoria")
		@Pattern(regexp = "^([A-Z]{3}[0-9]{3}|[A-Z]{2}[0-9]{3}[A-Z]{2})$",
				message = "La patente debe tener formato ABC123 o AB123CD")
		String patente,
		@NotBlank(message = "La marca es obligatoria") String marca,
		@NotBlank(message = "El modelo es obligatorio") String modelo,
		@NotNull(message = "El cliente es obligatorio") Long clienteId) {
}
