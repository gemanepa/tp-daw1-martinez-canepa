package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.service.RepuestoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepuestoPersistenciaTests {

	private final RepuestoService repuestoService;
	private final RepuestoRepository repuestoRepository;
	private final EntityManager entityManager;

	@Autowired
	RepuestoPersistenciaTests(RepuestoService repuestoService, RepuestoRepository repuestoRepository,
			EntityManager entityManager) {
		this.repuestoService = repuestoService;
		this.repuestoRepository = repuestoRepository;
		this.entityManager = entityManager;
	}

	@Test
	void crearRepuestoPersisteNombrePrecioConCentavosYStock() {
		Repuesto repuesto = new Repuesto();
		repuesto.setNombre("Filtro de aceite");
		repuesto.setPrecio(new BigDecimal("8500.75"));
		repuesto.setStock(10);

		Repuesto creado = repuestoService.crear(repuesto);
		assertThat(creado.getId()).isNotNull();
		entityManager.flush();
		entityManager.clear();

		Repuesto recuperado = repuestoRepository.findById(creado.getId()).orElseThrow();
		assertThat(recuperado.getNombre()).isEqualTo("Filtro de aceite");
		assertThat(recuperado.getPrecio()).isEqualByComparingTo("8500.75");
		assertThat(recuperado.getStock()).isEqualTo(10);
	}

}
