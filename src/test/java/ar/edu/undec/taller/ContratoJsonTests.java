package ar.edu.undec.taller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.repository.ClienteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ContratoJsonTests {

	private final JsonMapper jsonMapper;
	private final ClienteRepository clienteRepository;

	@Autowired
	ContratoJsonTests(JsonMapper jsonMapper, ClienteRepository clienteRepository) {
		this.jsonMapper = jsonMapper;
		this.clienteRepository = clienteRepository;
	}

	@Test
	@Transactional
	void incluirIdYDatosDelClienteEnElJson() {
		var cliente = new Cliente();
		cliente.setNombre("Ana Díaz");
		cliente.setTelefono("3825000000");
		clienteRepository.saveAndFlush(cliente);

		var json = jsonMapper.readTree(jsonMapper.writeValueAsString(cliente));

		assertThat(cliente.getId()).isNotNull();
		assertThat(json.get("id").longValue()).isEqualTo(cliente.getId());
		assertThat(json.get("nombre").stringValue()).isEqualTo("Ana Díaz");
		assertThat(json.get("telefono").stringValue()).isEqualTo("3825000000");
	}

	@Test
	void usarCamelCaseYFechasIsoEnElJson() {
		var datos = new DatosConFechas(
				LocalDate.of(2026, 11, 20),
				LocalDateTime.of(2026, 11, 20, 18, 30, 45));

		var contenido = jsonMapper.writeValueAsString(datos);
		var json = jsonMapper.readTree(contenido);

		assertThat(json.get("fecha").stringValue()).isEqualTo("2026-11-20");
		assertThat(json.get("fechaHora").stringValue()).isEqualTo("2026-11-20T18:30:45");
		assertThat(jsonMapper.readValue(contenido, DatosConFechas.class)).isEqualTo(datos);
	}

	record DatosConFechas(LocalDate fecha, LocalDateTime fechaHora) {
	}

}
