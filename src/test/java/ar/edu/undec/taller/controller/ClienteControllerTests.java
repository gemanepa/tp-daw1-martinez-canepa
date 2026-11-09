package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.repository.ClienteRepository;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteControllerTests {

	private final MockMvc mockMvc;
	private final ClienteRepository clienteRepository;
	private final JsonMapper jsonMapper;

	@Autowired
	ClienteControllerTests(MockMvc mockMvc, ClienteRepository clienteRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.clienteRepository = clienteRepository;
		this.jsonMapper = jsonMapper;
	}

	@Test
	@Transactional
	void crearClienteDevuelve201ObjetoCreadoYLocation() throws Exception {
		var respuesta = mockMvc.perform(post("/api/clientes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Carlos Ruiz","telefono":"3825123456"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Carlos Ruiz"))
				.andExpect(jsonPath("$.telefono").value("3825123456"))
				.andReturn().getResponse();

		long id = jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
		assertThat(respuesta.getHeader("Location")).isEqualTo("/api/clientes/" + id);
		assertThat(clienteRepository.findById(id)).hasValueSatisfying(cliente -> {
			assertThat(cliente.getNombre()).isEqualTo("Carlos Ruiz");
			assertThat(cliente.getTelefono()).isEqualTo("3825123456");
		});
	}

	@Test
	@Transactional
	void jsonMalFormadoDevuelve400ConProblemDetailSinGuardarCliente() throws Exception {
		long cantidadAnterior = clienteRepository.count();

		mockMvc.perform(post("/api/clientes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Carlos Ruiz\",\"telefono\":\"3825123456\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		assertThat(clienteRepository.count()).isEqualTo(cantidadAnterior);
	}

	@ParameterizedTest
	@MethodSource("clientesInvalidos")
	@Transactional
	void datosInvalidosDevuelven400ConErroresSinGuardarCliente(
			String contenido, Map<String, String> erroresEsperados) throws Exception {
		long cantidadAnterior = clienteRepository.count();

		mockMvc.perform(post("/api/clientes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"))
				.andExpect(jsonPath("$.errores").value(equalTo(erroresEsperados)));

		assertThat(clienteRepository.count()).isEqualTo(cantidadAnterior);
	}

	static Stream<Arguments> clientesInvalidos() {
		return Stream.of(
				Arguments.of("{\"nombre\":\"\",\"telefono\":\"3825123456\"}",
						Map.of("nombre", "El nombre es obligatorio")),
				Arguments.of("{\"nombre\":\"Carlos Ruiz\",\"telefono\":\"\"}",
						Map.of("telefono", "El teléfono es obligatorio")),
				Arguments.of("{\"telefono\":\"3825123456\"}",
						Map.of("nombre", "El nombre es obligatorio")),
				Arguments.of("{\"nombre\":\"Carlos Ruiz\"}",
						Map.of("telefono", "El teléfono es obligatorio")),
				Arguments.of("{\"nombre\":\"\",\"telefono\":\"\"}",
						Map.of("nombre", "El nombre es obligatorio", "telefono", "El teléfono es obligatorio")),
				Arguments.of("{}",
						Map.of("nombre", "El nombre es obligatorio", "telefono", "El teléfono es obligatorio")));
	}

}
