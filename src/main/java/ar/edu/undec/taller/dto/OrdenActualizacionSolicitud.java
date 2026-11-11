package ar.edu.undec.taller.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record OrdenActualizacionSolicitud(
		@NotBlank(message = "La descripción es obligatoria") String descripcion,
		BigDecimal costoFinal) {
}
