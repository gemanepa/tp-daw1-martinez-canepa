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

import static org.assertj.core.api.Assertions.assertThat;
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

	@Test
	void rechazarPatenteDeOtroAlActualizarSinModificarElVehiculo() {
		Cliente propietario = new Cliente();
		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente("ABC123");
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(propietario);
		var solicitud = new VehiculoSolicitud("AB123CD", "VW", "Gol", 2L);
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
		when(clienteRepository.findById(2L)).thenReturn(Optional.of(new Cliente()));
		when(vehiculoRepository.existsByPatenteAndIdNot("AB123CD", 1L)).thenReturn(true);

		assertThatThrownBy(() -> vehiculoService.actualizar(1L, solicitud))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("Ya existe un vehículo con la patente AB123CD");
		assertThat(vehiculo.getPatente()).isEqualTo("ABC123");
		assertThat(vehiculo.getMarca()).isEqualTo("Fiat");
		assertThat(vehiculo.getModelo()).isEqualTo("Cronos");
		assertThat(vehiculo.getCliente()).isSameAs(propietario);
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

}
