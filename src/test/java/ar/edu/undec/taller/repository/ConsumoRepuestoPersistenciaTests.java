package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.dto.ConsumoRepuestoSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.ConsumoRepuestoService;
import ar.edu.undec.taller.service.OrdenService;
import ar.edu.undec.taller.service.RepuestoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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
			JdbcTemplate jdbcTemplate) {
		this.clienteRepository = clienteRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.ordenService = ordenService;
		this.repuestoService = repuestoService;
		this.consumoRepuestoService = consumoRepuestoService;
		this.repuestoRepository = repuestoRepository;
		this.jdbcTemplate = jdbcTemplate;
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

}
