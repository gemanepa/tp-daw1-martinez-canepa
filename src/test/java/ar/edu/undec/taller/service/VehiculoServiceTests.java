package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTests {

	@Mock
	private VehiculoRepository vehiculoRepository;

	@Mock
	private ClienteRepository clienteRepository;

	@InjectMocks
	private VehiculoService vehiculoService;

	@Test
	void rechazarClienteInexistenteSinGuardarVehiculo() {
		var solicitud = new VehiculoSolicitud("ABC123", "Fiat", "Cronos", 1L);
		when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> vehiculoService.crear(solicitud))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessage("No existe el cliente con id 1");
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

	@Test
	void rechazarPatenteRepetidaSinGuardarVehiculo() {
		var solicitud = new VehiculoSolicitud("ABC123", "Fiat", "Cronos", 1L);
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(new Cliente()));
		when(vehiculoRepository.existsByPatente("ABC123")).thenReturn(true);

		assertThatThrownBy(() -> vehiculoService.crear(solicitud))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("Ya existe un vehículo con la patente ABC123");
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

}
