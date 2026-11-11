package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTests {

	@Mock
	private OrdenRepository ordenRepository;

	@Mock
	private VehiculoRepository vehiculoRepository;

	@InjectMocks
	private OrdenService ordenService;

	@Test
	void asignarEstadoIngresadoYFechaDesdeElSistema() {
		Vehiculo vehiculo = new Vehiculo();
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
		when(ordenRepository.save(any(Orden.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		LocalDateTime antes = LocalDateTime.now();

		Orden orden = ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite"));

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.INGRESADO);
		assertThat(orden.getFechaIngreso()).isBetween(antes, LocalDateTime.now());
		assertThat(orden.getDescripcion()).isEqualTo("Cambio de aceite");
		assertThat(orden.getVehiculo()).isSameAs(vehiculo);
	}

	@Test
	void rechazarVehiculoInexistenteSinGuardarOrden() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite")))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessage("No existe el vehículo con id 1");
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@Test
	void rechazarOtraOrdenAbiertaSinGuardarOrden() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(new Vehiculo()));
		when(ordenRepository.existsByVehiculo_IdAndEstadoNotIn(1L,
				List.of(EstadoOrden.ENTREGADO, EstadoOrden.CANCELADO))).thenReturn(true);

		assertThatThrownBy(() -> ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite")))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("El vehículo ya tiene una orden abierta");
		verify(ordenRepository, never()).save(any(Orden.class));
	}

}
