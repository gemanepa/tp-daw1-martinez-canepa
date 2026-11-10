package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VehiculoControllerTests {

	private final MockMvc mockMvc;
	private final ClienteRepository clienteRepository;
	private final VehiculoRepository vehiculoRepository;
	private final JsonMapper jsonMapper;
	private Cliente cliente;

	@Autowired
	VehiculoControllerTests(MockMvc mockMvc, ClienteRepository clienteRepository,
			VehiculoRepository vehiculoRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.clienteRepository = clienteRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.jsonMapper = jsonMapper;
	}

	@BeforeEach
	void crearCliente() {
		cliente = guardarCliente("Carlos Ruiz");
	}

	@ParameterizedTest
	@ValueSource(strings = {"ABC123", "AB123CD"})
	void crearVehiculoDevuelve201ObjetoClienteIdYLocation(String patente) throws Exception {
		var respuesta = mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(patente, cliente.getId())))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.patente").value(patente))
				.andExpect(jsonPath("$.marca").value("Fiat"))
				.andExpect(jsonPath("$.modelo").value("Cronos"))
				.andExpect(jsonPath("$.clienteId").value(cliente.getId().intValue()))
				.andExpect(jsonPath("$.cliente").doesNotExist())
				.andReturn().getResponse();

		long id = jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
		assertThat(respuesta.getHeader("Location")).isEqualTo("/api/vehiculos/" + id);
		assertThat(vehiculoRepository.findById(id)).hasValueSatisfying(vehiculo -> {
			assertThat(vehiculo.getPatente()).isEqualTo(patente);
			assertThat(vehiculo.getClienteId()).isEqualTo(cliente.getId());
		});
	}

	@ParameterizedTest
	@MethodSource("vehiculosInvalidos")
	void datosInvalidosDevuelven400ConErroresSinGuardarVehiculo(
			String contenido, Set<String> camposConError) throws Exception {
		long cantidadAnterior = vehiculoRepository.count();

		var resultado = mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido.formatted(cliente.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"));

		for (String campo : camposConError) {
			resultado.andExpect(jsonPath("$.errores." + campo).isNotEmpty());
		}
		assertThat(vehiculoRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void clienteInexistenteDevuelve404SinGuardarVehiculo() throws Exception {
		long cantidadAnterior = vehiculoRepository.count();

		mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("ABC123", 999999L)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el cliente con id 999999"));

		assertThat(vehiculoRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void patenteRepetidaEntreClientesDevuelve409SinGuardarOtroVehiculo() throws Exception {
		mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("ABC123", cliente.getId())))
				.andExpect(status().isCreated());
		Cliente otroCliente = guardarCliente("Ana Díaz");
		long cantidadAnterior = vehiculoRepository.count();

		mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("ABC123", otroCliente.getId())))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("Ya existe un vehículo con la patente ABC123"));

		assertThat(vehiculoRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void jsonMalFormadoDevuelve400SinGuardarVehiculo() throws Exception {
		long cantidadAnterior = vehiculoRepository.count();

		mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"patente\":\"ABC123\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		assertThat(vehiculoRepository.count()).isEqualTo(cantidadAnterior);
	}

	static Stream<Arguments> vehiculosInvalidos() {
		return Stream.of(
				Arguments.of("{\"patente\":\"123\",\"marca\":\"\",\"modelo\":\"\",\"clienteId\":null}",
						Set.of("patente", "marca", "modelo", "clienteId")),
				Arguments.of("{}", Set.of("patente", "marca", "modelo", "clienteId")),
				Arguments.of("{\"patente\":\"\",\"marca\":\"Fiat\",\"modelo\":\"Cronos\",\"clienteId\":%d}",
						Set.of("patente")),
				Arguments.of("{\"patente\":\"ABC123\",\"modelo\":\"Cronos\",\"clienteId\":%d}",
						Set.of("marca")),
				Arguments.of("{\"patente\":\"ABC123\",\"marca\":\"Fiat\",\"clienteId\":%d}",
						Set.of("modelo")),
				Arguments.of("{\"patente\":\"ABC123\",\"marca\":\"Fiat\",\"modelo\":\"Cronos\"}",
						Set.of("clienteId")));
	}

	private Cliente guardarCliente(String nombre) {
		Cliente nuevo = new Cliente();
		nuevo.setNombre(nombre);
		nuevo.setTelefono("3825123456");
		return clienteRepository.saveAndFlush(nuevo);
	}

	private String contenidoValido(String patente, Long clienteId) {
		return """
				{"patente":"%s","marca":"Fiat","modelo":"Cronos","clienteId":%d}
				""".formatted(patente, clienteId);
	}

}
