package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.repository.ConsumoRepuestoRepository;
import ar.edu.undec.taller.repository.RepuestoRepository;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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
class ConsumoRepuestoControllerTests {

	private final MockMvc mockMvc;
	private final JsonMapper jsonMapper;
	private final ConsumoRepuestoRepository consumoRepuestoRepository;
	private final RepuestoRepository repuestoRepository;
	private final EntityManager entityManager;
	private long ordenId;
	private long repuestoId;

	@Autowired
	ConsumoRepuestoControllerTests(MockMvc mockMvc, JsonMapper jsonMapper,
			ConsumoRepuestoRepository consumoRepuestoRepository, RepuestoRepository repuestoRepository,
			EntityManager entityManager) {
		this.mockMvc = mockMvc;
		this.jsonMapper = jsonMapper;
		this.consumoRepuestoRepository = consumoRepuestoRepository;
		this.repuestoRepository = repuestoRepository;
		this.entityManager = entityManager;
	}

	@BeforeEach
	void crearOrdenYRepuestoPorApi() throws Exception {
		long clienteId = crearPorApi("/api/clientes",
				"{\"nombre\":\"Carlos Ruiz\",\"telefono\":\"3825123456\"}");
		long vehiculoId = crearPorApi("/api/vehiculos", """
				{"patente":"ABC123","marca":"Fiat","modelo":"Cronos","clienteId":%d}
				""".formatted(clienteId));
		ordenId = crearPorApi("/api/ordenes",
				"{\"vehiculoId\":%d,\"descripcion\":\"Cambio de aceite\"}".formatted(vehiculoId));
		repuestoId = crearPorApi("/api/repuestos",
				"{\"nombre\":\"Filtro de aceite\",\"precio\":8500.75,\"stock\":10}");
	}

	@Test
	void registrarConsumoDevuelve201ConIdRelacionesCantidadYLocation() throws Exception {
		var respuesta = mockMvc.perform(post("/api/ordenes/{id}/repuestos", ordenId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"repuestoId\":%d,\"cantidad\":3}".formatted(repuestoId)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.ordenId").value((int) ordenId))
				.andExpect(jsonPath("$.repuestoId").value((int) repuestoId))
				.andExpect(jsonPath("$.cantidad").value(3))
				.andExpect(jsonPath("$.orden").doesNotExist())
				.andExpect(jsonPath("$.repuesto").doesNotExist())
				.andReturn().getResponse();

		long consumoId = jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
		assertThat(respuesta.getHeader("Location"))
				.isEqualTo("/api/ordenes/" + ordenId + "/repuestos/" + consumoId);
		entityManager.flush();
		entityManager.clear();
		assertThat(consumoRepuestoRepository.findById(consumoId)).hasValueSatisfying(consumo -> {
			assertThat(consumo.getOrdenId()).isEqualTo(ordenId);
			assertThat(consumo.getRepuestoId()).isEqualTo(repuestoId);
			assertThat(consumo.getCantidad()).isEqualTo(3);
		});
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(7);
	}

	@Test
	void ordenInexistenteDevuelve404SinCambiarDatos() throws Exception {
		mockMvc.perform(post("/api/ordenes/999999/repuestos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"repuestoId\":%d,\"cantidad\":3}".formatted(repuestoId)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe la orden con id 999999"));

		comprobarSinCambios();
	}

	@Test
	void repuestoInexistenteDevuelve404SinCambiarDatos() throws Exception {
		mockMvc.perform(post("/api/ordenes/{id}/repuestos", ordenId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"repuestoId\":999999,\"cantidad\":3}"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el repuesto con id 999999"));

		comprobarSinCambios();
	}

	@Test
	void stockInsuficienteDevuelve409SinCambiarDatos() throws Exception {
		mockMvc.perform(post("/api/ordenes/{id}/repuestos", ordenId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"repuestoId\":%d,\"cantidad\":11}".formatted(repuestoId)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("No hay stock suficiente del repuesto con id " + repuestoId));

		comprobarSinCambios();
	}

	@ParameterizedTest
	@MethodSource("consumosInvalidos")
	void datosInvalidosDevuelven400ConErroresSinCambiarDatos(
			String contenido, Map<String, String> erroresEsperados) throws Exception {
		mockMvc.perform(post("/api/ordenes/{id}/repuestos", ordenId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido.formatted(repuestoId)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"))
				.andExpect(jsonPath("$.errores").value(equalTo(erroresEsperados)));

		comprobarSinCambios();
	}

	@Test
	void idNoNumericoDevuelve400SinCambiarDatos() throws Exception {
		mockMvc.perform(post("/api/ordenes/abc/repuestos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"repuestoId\":%d,\"cantidad\":3}".formatted(repuestoId)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));

		comprobarSinCambios();
	}

	@Test
	void jsonMalFormadoDevuelve400SinCambiarDatos() throws Exception {
		mockMvc.perform(post("/api/ordenes/{id}/repuestos", ordenId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cantidad\":3"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarSinCambios();
	}

	static Stream<Arguments> consumosInvalidos() {
		return Stream.of(
				Arguments.of("{}", Map.of("repuestoId", "El repuesto es obligatorio",
						"cantidad", "La cantidad es obligatoria")),
				Arguments.of("{\"repuestoId\":%d}", Map.of("cantidad", "La cantidad es obligatoria")),
				Arguments.of("{\"repuestoId\":%d,\"cantidad\":0}",
						Map.of("cantidad", "La cantidad debe ser mayor que cero")),
				Arguments.of("{\"repuestoId\":%d,\"cantidad\":-1}",
						Map.of("cantidad", "La cantidad debe ser mayor que cero")));
	}

	private long crearPorApi(String ruta, String contenido) throws Exception {
		var respuesta = mockMvc.perform(post(ruta)
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
		return jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
	}

	private void comprobarSinCambios() {
		entityManager.flush();
		entityManager.clear();
		assertThat(consumoRepuestoRepository.count()).isZero();
		assertThat(repuestoRepository.findById(repuestoId).orElseThrow().getStock()).isEqualTo(10);
	}

}
