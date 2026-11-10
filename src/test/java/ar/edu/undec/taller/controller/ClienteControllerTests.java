package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
	private final VehiculoRepository vehiculoRepository;
	private final JsonMapper jsonMapper;

	@Autowired
	ClienteControllerTests(MockMvc mockMvc, ClienteRepository clienteRepository,
			VehiculoRepository vehiculoRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.clienteRepository = clienteRepository;
		this.vehiculoRepository = vehiculoRepository;
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

	@Test
	@Transactional
	void listarVehiculosDelClienteDevuelve200YFiltraPorRelacion() throws Exception {
		Cliente cliente = guardarCliente("Carlos Ruiz");
		Cliente otroCliente = guardarCliente("Ana Díaz");
		Vehiculo primero = guardarVehiculo("ABC123", cliente);
		Vehiculo segundo = guardarVehiculo("AB123CD", cliente);
		guardarVehiculo("DEF456", otroCliente);
		String jsonEsperado = """
				[
				  {"id":%d,"patente":"ABC123","marca":"Fiat","modelo":"Cronos","clienteId":%d},
				  {"id":%d,"patente":"AB123CD","marca":"Fiat","modelo":"Cronos","clienteId":%d}
				]
				""".formatted(primero.getId(), cliente.getId(), segundo.getId(), cliente.getId());

		mockMvc.perform(get("/api/clientes/{id}/vehiculos", cliente.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(content().json(jsonEsperado));
	}

	@Test
	@Transactional
	void clienteSinVehiculosDevuelve200YArrayVacioAunqueOtroTengaVehiculos() throws Exception {
		Cliente cliente = guardarCliente("Carlos Ruiz");
		Cliente otroCliente = guardarCliente("Ana Díaz");
		guardarVehiculo("ABC123", otroCliente);

		mockMvc.perform(get("/api/clientes/{id}/vehiculos", cliente.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	@Transactional
	void listarVehiculosDeClienteInexistenteDevuelve404ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/clientes/999999/vehiculos"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el cliente con id 999999"));
	}

	@Test
	@Transactional
	void listarVehiculosConIdNoNumericoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/clientes/abc/vehiculos"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));
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

	private Cliente guardarCliente(String nombre) {
		Cliente cliente = new Cliente();
		cliente.setNombre(nombre);
		cliente.setTelefono("3825123456");
		return clienteRepository.saveAndFlush(cliente);
	}

	private Vehiculo guardarVehiculo(String patente, Cliente cliente) {
		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente(patente);
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(cliente);
		return vehiculoRepository.saveAndFlush(vehiculo);
	}

}
