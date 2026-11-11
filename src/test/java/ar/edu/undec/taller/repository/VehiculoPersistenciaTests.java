package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.VehiculoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehiculoPersistenciaTests {

	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;
	private final EntityManager entityManager;
	private final VehiculoService vehiculoService;
	private final OrdenRepository ordenRepository;

	@Autowired
	VehiculoPersistenciaTests(VehiculoRepository vehiculoRepository,
			ClienteRepository clienteRepository, EntityManager entityManager, VehiculoService vehiculoService,
			OrdenRepository ordenRepository) {
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.entityManager = entityManager;
		this.vehiculoService = vehiculoService;
		this.ordenRepository = ordenRepository;
	}

	@Test
	void guardarYRecuperarVehiculoConSuCliente() {
		Cliente cliente = guardarCliente("Ana Díaz");
		Vehiculo vehiculo = vehiculoRepository.saveAndFlush(nuevoVehiculo(cliente));
		Long id = vehiculo.getId();
		entityManager.clear();

		Vehiculo recuperado = vehiculoRepository.findById(id).orElseThrow();

		assertThat(recuperado.getPatente()).isEqualTo("ABC123");
		assertThat(recuperado.getMarca()).isEqualTo("Fiat");
		assertThat(recuperado.getModelo()).isEqualTo("Cronos");
		assertThat(recuperado.getClienteId()).isEqualTo(cliente.getId());
		assertThat(recuperado.getCliente().getNombre()).isEqualTo("Ana Díaz");
	}

	@Test
	void laBaseRechazaPatentesRepetidasInclusoEntreClientesDistintos() {
		Cliente primero = guardarCliente("Ana Díaz");
		Cliente segundo = guardarCliente("Carlos Ruiz");
		vehiculoRepository.saveAndFlush(nuevoVehiculo(primero));

		assertThatThrownBy(() -> vehiculoRepository.saveAndFlush(nuevoVehiculo(segundo)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void actualizarConservaIdYGuardaLosDatosYLaRelacionConOtroCliente() {
		Cliente propietario = guardarCliente("Ana Díaz");
		Cliente otroPropietario = guardarCliente("Carlos Ruiz");
		Vehiculo vehiculo = vehiculoRepository.saveAndFlush(nuevoVehiculo(propietario));
		Long id = vehiculo.getId();
		entityManager.clear();

		vehiculoService.actualizar(id,
				new VehiculoSolicitud("AB123CD", "VW", "Gol", otroPropietario.getId()));
		entityManager.flush();
		entityManager.clear();

		Vehiculo recuperado = vehiculoRepository.findById(id).orElseThrow();
		assertThat(recuperado.getId()).isEqualTo(id);
		assertThat(recuperado.getPatente()).isEqualTo("AB123CD");
		assertThat(recuperado.getMarca()).isEqualTo("VW");
		assertThat(recuperado.getModelo()).isEqualTo("Gol");
		assertThat(recuperado.getClienteId()).isEqualTo(otroPropietario.getId());
		assertThat(vehiculoRepository.count()).isEqualTo(1);
	}

	@Test
	void eliminarVehiculoPersisteElBorradoYConservaAlCliente() {
		Cliente cliente = guardarCliente("Ana Díaz");
		Vehiculo vehiculo = vehiculoRepository.saveAndFlush(nuevoVehiculo(cliente));

		vehiculoService.eliminar(vehiculo.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(vehiculoRepository.findById(vehiculo.getId())).isEmpty();
		assertThat(clienteRepository.findById(cliente.getId())).isPresent();
	}

	@ParameterizedTest
	@EnumSource(EstadoOrden.class)
	void rechazarBorradoConservaElVehiculoYSuOrdenEnCualquierEstado(EstadoOrden estado) {
		Cliente cliente = guardarCliente("Ana Díaz");
		Vehiculo vehiculo = vehiculoRepository.saveAndFlush(nuevoVehiculo(cliente));
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Cambio de aceite");
		orden.setEstado(estado);
		orden.setFechaIngreso(LocalDateTime.of(2026, 11, 10, 9, 0));
		orden.setCostoFinal(new BigDecimal("25000.00"));
		ordenRepository.saveAndFlush(orden);
		entityManager.clear();

		assertThatThrownBy(() -> vehiculoService.eliminar(vehiculo.getId()))
				.isInstanceOf(OperacionNoPermitidaException.class);
		entityManager.flush();
		entityManager.clear();

		Vehiculo conservado = vehiculoRepository.findById(vehiculo.getId()).orElseThrow();
		assertThat(conservado).usingRecursiveComparison().ignoringFields("cliente").isEqualTo(vehiculo);
		assertThat(conservado.getClienteId()).isEqualTo(cliente.getId());
		Orden conservada = ordenRepository.findById(orden.getId()).orElseThrow();
		assertThat(conservada).usingRecursiveComparison().ignoringFields("vehiculo").isEqualTo(orden);
		assertThat(conservada.getVehiculoId()).isEqualTo(vehiculo.getId());
	}

	private Cliente guardarCliente(String nombre) {
		Cliente cliente = new Cliente();
		cliente.setNombre(nombre);
		cliente.setTelefono("3825123456");
		return clienteRepository.saveAndFlush(cliente);
	}

	private Vehiculo nuevoVehiculo(Cliente cliente) {
		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente("ABC123");
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(cliente);
		return vehiculo;
	}

}
