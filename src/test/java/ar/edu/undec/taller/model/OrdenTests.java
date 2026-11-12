package ar.edu.undec.taller.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrdenTests {

	@ParameterizedTest
	@CsvSource({
			"2026-11-10T10:00:00, 2026-11-10T12:00:00, 2.0",
			"2026-11-10T10:15:00, 2026-11-10T12:45:00, 2.5",
			"2026-11-10T10:15:00, 2026-11-11T12:45:00, 26.5"
	})
	void calcularHorasDeReparacionConFechasConocidas(String inicio, String terminacion, double horas) {
		Orden orden = new Orden();
		orden.setEstado(EstadoOrden.TERMINADO);
		orden.setFechaInicioReparacion(LocalDateTime.parse(inicio));
		orden.setFechaTerminacion(LocalDateTime.parse(terminacion));

		assertThat(orden.getHorasEnReparacion()).isEqualTo(horas);
	}

	@Test
	void noCalcularHorasAntesDeTerminarLaReparacion() {
		Orden orden = new Orden();
		orden.setEstado(EstadoOrden.INGRESADO);
		assertThat(orden.getHorasEnReparacion()).isNull();

		orden.setEstado(EstadoOrden.EN_REPARACION);
		orden.setFechaInicioReparacion(LocalDateTime.of(2026, 11, 10, 10, 0));
		assertThat(orden.getHorasEnReparacion()).isNull();
	}

}
