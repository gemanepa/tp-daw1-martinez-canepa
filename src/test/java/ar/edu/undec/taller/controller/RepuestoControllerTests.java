package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.repository.RepuestoRepository;
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
@Transactional
class RepuestoControllerTests {

	private final MockMvc mockMvc;
	private final RepuestoRepository repuestoRepository;
	private final JsonMapper jsonMapper;

	@Autowired
	RepuestoControllerTests(MockMvc mockMvc, RepuestoRepository repuestoRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.repuestoRepository = repuestoRepository;
		this.jsonMapper = jsonMapper;
	}

	@Test
	void crearRepuestoDevuelve201ObjetoCreadoYLocation() throws Exception {
		var respuesta = mockMvc.perform(post("/api/repuestos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Filtro de aceite","precio":8500.75,"stock":10}
						"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Filtro de aceite"))
				.andExpect(jsonPath("$.precio").value(8500.75))
				.andExpect(jsonPath("$.stock").value(10))
				.andReturn().getResponse();

		long id = jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
		assertThat(respuesta.getHeader("Location")).isEqualTo("/api/repuestos/" + id);
		assertThat(repuestoRepository.findById(id)).hasValueSatisfying(repuesto -> {
			assertThat(repuesto.getNombre()).isEqualTo("Filtro de aceite");
			assertThat(repuesto.getPrecio()).isEqualByComparingTo("8500.75");
			assertThat(repuesto.getStock()).isEqualTo(10);
		});
	}

	@ParameterizedTest
	@MethodSource("repuestosInvalidos")
	void datosInvalidosDevuelven400ConErroresSinGuardarRepuesto(
			String contenido, Map<String, String> erroresEsperados) throws Exception {
		long cantidadAnterior = repuestoRepository.count();

		mockMvc.perform(post("/api/repuestos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"))
				.andExpect(jsonPath("$.errores").value(equalTo(erroresEsperados)));

		assertThat(repuestoRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void jsonMalFormadoDevuelve400ConProblemDetailSinGuardarRepuesto() throws Exception {
		long cantidadAnterior = repuestoRepository.count();

		mockMvc.perform(post("/api/repuestos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Filtro de aceite\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		assertThat(repuestoRepository.count()).isEqualTo(cantidadAnterior);
	}

	static Stream<Arguments> repuestosInvalidos() {
		return Stream.of(
				Arguments.of("{}", Map.of("nombre", "El nombre es obligatorio",
						"precio", "El precio es obligatorio", "stock", "El stock es obligatorio")),
				Arguments.of("{\"nombre\":\"\",\"precio\":8500.75,\"stock\":10}",
						Map.of("nombre", "El nombre es obligatorio")));
	}

}
