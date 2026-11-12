package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.dto.ConsumoRepuestoSolicitud;
import ar.edu.undec.taller.dto.OrdenActualizacionSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.exception.CostoFinalInvalidoException;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.ConsumoRepuestoService;
import ar.edu.undec.taller.service.OrdenService;
import ar.edu.undec.taller.service.RepuestoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class ConsumoRepuestoPersistenciaTests {

	private final ClienteRepository clienteRepository;
	private final VehiculoRepository vehiculoRepository;
	private final OrdenService ordenService;
	private final RepuestoService repuestoService;
	private final ConsumoRepuestoService consumoRepuestoService;
	private final RepuestoRepository repuestoRepository;
	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;
	private Long clienteId;
	private Long vehiculoId;
	private Long ordenId;
	private Long repuestoId;

	@MockitoSpyBean
	private ConsumoRepuestoRepository consumoRepuestoRepository;

	@Autowired
	ConsumoRepuestoPersistenciaTests(ClienteRepository clienteRepository,
			VehiculoRepository vehiculoRepository, OrdenService ordenService, RepuestoService repuestoService,
			ConsumoRepuestoService consumoRepuestoService, RepuestoRepository repuestoRepository,
			JdbcTemplate jdbcTemplate, EntityManager entityManager) {
		this.clienteRepository = clienteRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.ordenService = ordenService;
		this.repuestoService = repuestoService;
		this.consumoRepuestoService = consumoRepuestoService;
		this.repuestoRepository = repuestoRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	@BeforeEach
	void crearOrdenYRepuesto() {
		Cliente cliente = new Cliente();
		cliente.setNombre("Carlos Ruiz");
		cliente.setTelefono("3825123456");
		clienteId = clienteRepository.saveAndFlush(cliente).getId();
		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente("ABC123");
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(cliente);
		vehiculoId = vehiculoRepository.saveAndFlush(vehiculo).getId();
		ordenId = ordenService.crear(new OrdenSolicitud(vehiculoId, "Cambio de aceite")).getId();
		Repuesto repuesto = new Repuesto();
		repuesto.setNombre("Filtro de aceite");
		repuesto.setPrecio(new BigDecimal("8500.75"));
		repuesto.setStock(10);
		repuestoId = repuestoService.crear(repuesto).getId();
	}

	@AfterEach
	void eliminarSoloLosDatosDeEstaPrueba() {
		if (ordenId != null) {
			jdbcTemplate.update("delete from consumo_repuesto where orden_id = ?", ordenId);
			jdbcTemplate.update("delete from orden where id = ?", ordenId);
		}
		if (vehiculoId != null) {
			jdbcTemplate.update("delete from vehiculo where id = ?", vehiculoId);
		}
		if (clienteId != null) {
			jdbcTemplate.update("delete from cliente where id = ?", clienteId);
		}
		if (repuestoId != null) {
			jdbcTemplate.update("delete from repuesto where id = ?", repuestoId);
		}
	}

	@Test
	void registrarConfirmaLaAsociacionYElDescuentoJuntos() {
		ConsumoRepuesto creado = consumoRepuestoService.registrar(ordenId,
				new ConsumoRepuestoSolicitud(repuestoId, 3));

		ConsumoRepuesto recuperado = consumoRepuestoRepository.findById(creado.getId()).orElseThrow();
		assertThat(recuperado.getCantidad()).isEqualTo(3);
		assertThat(recuperado.getOrdenId()).isEqualTo(ordenId);
		assertThat(recuperado.getOrden().getVehiculoId()).isEqualTo(vehiculoId);
		assertThat(recuperado.getRepuestoId()).isEqualTo(repuestoId);
		assertThat(recuperado.getRepuesto().getNombre()).isEqualTo("Filtro de aceite");
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(7);
	}

	@Test
	void stockInsuficienteNoGuardaConsumosNiCambiaExistencias() {
		long cantidadAnterior = consumoRepuestoRepository.count();

		assertThatThrownBy(() -> consumoRepuestoService.registrar(ordenId,
				new ConsumoRepuestoSolicitud(repuestoId, 11)))
				.isInstanceOf(OperacionNoPermitidaException.class);

		assertThat(consumoRepuestoRepository.count()).isEqualTo(cantidadAnterior);
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(10);
	}

	@Test
	void unFalloAlGuardarElConsumoRevierteElDescuentoDeStock() {
		long cantidadAnterior = consumoRepuestoRepository.count();
		doThrow(new DataIntegrityViolationException("Fallo al guardar el consumo"))
				.when(consumoRepuestoRepository).save(any(ConsumoRepuesto.class));

		assertThatThrownBy(() -> consumoRepuestoService.registrar(ordenId,
				new ConsumoRepuestoSolicitud(repuestoId, 3)))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(consumoRepuestoRepository.count()).isEqualTo(cantidadAnterior);
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(10);
	}

	@Test
	@Transactional
	void calcularCostoDesdeLosConsumosPersistidosIncluyeConsumosDelMismoRepuesto() {
		registrarDosConsumos();
		assertThat(ordenService.buscarPorId(ordenId).getCostoRepuestos()).isEqualByComparingTo("42503.75");
		entityManager.flush();
		entityManager.clear();

		Orden recuperada = ordenService.buscarPorId(ordenId);
		assertThat(recuperada.getConsumos()).hasSize(2);
		assertThat(recuperada.getCostoRepuestos()).isEqualByComparingTo("42503.75");
	}

	@ParameterizedTest
	@ValueSource(strings = {"42503.75", "45000.75"})
	@Transactional
	void actualizarPersisteUnCostoFinalSuficiente(String costoFinal) {
		registrarDosConsumos();
		entityManager.flush();
		entityManager.clear();

		ordenService.actualizar(ordenId,
				new OrdenActualizacionSolicitud("Reparación completa", new BigDecimal(costoFinal)));
		entityManager.flush();
		entityManager.clear();

		Orden recuperada = ordenService.buscarPorId(ordenId);
		assertThat(recuperada.getDescripcion()).isEqualTo("Reparación completa");
		assertThat(recuperada.getCostoFinal()).isEqualByComparingTo(costoFinal);
		assertThat(recuperada.getCostoRepuestos()).isEqualByComparingTo("42503.75");
	}

	@Test
	@Transactional
	void costoFinalInferiorConservaLaOrdenLosConsumosYElStockPersistidos() {
		ordenService.actualizar(ordenId,
				new OrdenActualizacionSolicitud("Trabajo anterior", new BigDecimal("45000.75")));
		registrarDosConsumos();
		entityManager.flush();
		entityManager.clear();

		assertThatThrownBy(() -> ordenService.actualizar(ordenId,
				new OrdenActualizacionSolicitud("Cambio rechazado", new BigDecimal("42503.74"))))
				.isInstanceOf(CostoFinalInvalidoException.class);
		entityManager.flush();
		entityManager.clear();

		Orden recuperada = ordenService.buscarPorId(ordenId);
		assertThat(recuperada.getDescripcion()).isEqualTo("Trabajo anterior");
		assertThat(recuperada.getCostoFinal()).isEqualByComparingTo("45000.75");
		assertThat(recuperada.getCostoRepuestos()).isEqualByComparingTo("42503.75");
		assertThat(recuperada.getConsumos()).hasSize(2);
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(5);
	}

	private void registrarDosConsumos() {
		entityManager.flush();
		entityManager.clear();
		consumoRepuestoService.registrar(ordenId, new ConsumoRepuestoSolicitud(repuestoId, 2));
		consumoRepuestoService.registrar(ordenId, new ConsumoRepuestoSolicitud(repuestoId, 3));
	}

}
