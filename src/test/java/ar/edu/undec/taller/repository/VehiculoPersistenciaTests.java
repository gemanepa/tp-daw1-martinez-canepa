package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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

	@Autowired
	VehiculoPersistenciaTests(VehiculoRepository vehiculoRepository,
			ClienteRepository clienteRepository, EntityManager entityManager) {
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.entityManager = entityManager;
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
