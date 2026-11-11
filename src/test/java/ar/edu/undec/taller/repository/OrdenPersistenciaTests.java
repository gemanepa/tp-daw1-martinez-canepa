package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.dto.OrdenActualizacionSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.OrdenService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrdenPersistenciaTests {

	private final OrdenRepository ordenRepository;
	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;
	private final OrdenService ordenService;
	private final EntityManager entityManager;
	private Vehiculo vehiculo;

	@Autowired
	OrdenPersistenciaTests(OrdenRepository ordenRepository, VehiculoRepository vehiculoRepository,
			ClienteRepository clienteRepository, OrdenService ordenService, EntityManager entityManager) {
		this.ordenRepository = ordenRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.ordenService = ordenService;
		this.entityManager = entityManager;
	}

	@BeforeEach
	void crearVehiculoConCliente() {
		Cliente cliente = new Cliente();
		cliente.setNombre("Carlos Ruiz");
		cliente.setTelefono("3825123456");
		clienteRepository.saveAndFlush(cliente);
		vehiculo = new Vehiculo();
		vehiculo.setPatente("ABC123");
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(cliente);
		vehiculoRepository.saveAndFlush(vehiculo);
	}

	@Test
	void guardarYRecuperarOrdenConEstadoFechaYRelacionAlVehiculo() {
		LocalDateTime antes = LocalDateTime.now();
		Orden orden = ordenService.crear(new OrdenSolicitud(vehiculo.getId(), "Cambio de aceite"));
		LocalDateTime despues = LocalDateTime.now();
		Long id = orden.getId();
		entityManager.flush();
		entityManager.clear();

		Orden recuperada = ordenRepository.findById(id).orElseThrow();
		assertThat(recuperada.getId()).isEqualTo(id);
		assertThat(recuperada.getDescripcion()).isEqualTo("Cambio de aceite");
		assertThat(recuperada.getEstado()).isEqualTo(EstadoOrden.INGRESADO);
		assertThat(recuperada.getFechaIngreso()).isBetween(antes, despues);
		assertThat(recuperada.getVehiculoId()).isEqualTo(vehiculo.getId());
		assertThat(recuperada.getVehiculo().getPatente()).isEqualTo("ABC123");
	}

	@Test
	void actualizarGuardaDescripcionYCostoSinCambiarVehiculoEstadoNiFecha() {
		LocalDateTime fecha = LocalDateTime.of(2026, 11, 10, 10, 0);
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(EstadoOrden.EN_REPARACION);
		orden.setFechaIngreso(fecha);
		ordenRepository.saveAndFlush(orden);
		Long id = orden.getId();
		entityManager.clear();

		ordenService.actualizar(id,
				new OrdenActualizacionSolicitud("Cambio de aceite y filtro", new BigDecimal("45000.75")));
		entityManager.flush();
		entityManager.clear();

		Orden recuperada = ordenRepository.findById(id).orElseThrow();
		assertThat(recuperada.getId()).isEqualTo(id);
		assertThat(recuperada.getDescripcion()).isEqualTo("Cambio de aceite y filtro");
		assertThat(recuperada.getCostoFinal()).isEqualByComparingTo("45000.75");
		assertThat(recuperada.getVehiculoId()).isEqualTo(vehiculo.getId());
		assertThat(recuperada.getEstado()).isEqualTo(EstadoOrden.EN_REPARACION);
		assertThat(recuperada.getFechaIngreso()).isEqualTo(fecha);
		assertThat(ordenRepository.count()).isEqualTo(1);
	}

	@ParameterizedTest
	@CsvSource({
			"INGRESADO, true",
			"EN_REPARACION, true",
			"TERMINADO, true",
			"ENTREGADO, false",
			"CANCELADO, false"
	})
	void consultarOrdenAbiertaSegunElEstadoPersistido(EstadoOrden estado, boolean abierta) {
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(estado);
		orden.setFechaIngreso(LocalDateTime.of(2026, 11, 10, 10, 0));
		ordenRepository.saveAndFlush(orden);
		entityManager.clear();

		assertThat(ordenRepository.existsByVehiculo_IdAndEstadoNotIn(vehiculo.getId(),
				List.of(EstadoOrden.ENTREGADO, EstadoOrden.CANCELADO))).isEqualTo(abierta);
	}

}
