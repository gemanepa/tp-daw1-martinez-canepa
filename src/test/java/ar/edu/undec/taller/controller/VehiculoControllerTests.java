package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	@Test
	void listarSinVehiculosDevuelve200YArrayVacio() throws Exception {
		mockMvc.perform(get("/api/vehiculos"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void listarVehiculosDevuelve200ConDatosEIdentificadores() throws Exception {
		Vehiculo primero = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);
		Cliente otroCliente = guardarCliente("Ana Díaz");
		Vehiculo segundo = guardarVehiculo("AB123CD", "VW", "Gol", otroCliente);
		String jsonEsperado = """
				[
				  {"id":%d,"patente":"ABC123","marca":"Fiat","modelo":"Cronos","clienteId":%d},
				  {"id":%d,"patente":"AB123CD","marca":"VW","modelo":"Gol","clienteId":%d}
				]
				""".formatted(primero.getId(), cliente.getId(), segundo.getId(), otroCliente.getId());

		mockMvc.perform(get("/api/vehiculos"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(content().json(jsonEsperado));
	}

	@Test
	void obtenerVehiculoPorIdDevuelve200ConObjetoGuardado() throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);

		mockMvc.perform(get("/api/vehiculos/{id}", vehiculo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$.id").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.patente").value("ABC123"))
				.andExpect(jsonPath("$.marca").value("Fiat"))
				.andExpect(jsonPath("$.modelo").value("Cronos"))
				.andExpect(jsonPath("$.clienteId").value(cliente.getId().intValue()));
	}

	@Test
	void obtenerVehiculoInexistenteDevuelve404ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/vehiculos/999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el vehículo con id 999999"));
	}

	@Test
	void idNoNumericoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/vehiculos/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));
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

	@ParameterizedTest
	@ValueSource(strings = {"ABC123", "AB123CD"})
	void actualizarDevuelve200YConservaIdConPatentePropiaONueva(String patente) throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);
		Cliente otroCliente = guardarCliente("Ana Díaz");
		String contenido = """
				{"patente":"%s","marca":"VW","modelo":"Gol","clienteId":%d}
				""".formatted(patente, otroCliente.getId());

		mockMvc.perform(put("/api/vehiculos/{id}", vehiculo.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$.id").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.patente").value(patente))
				.andExpect(jsonPath("$.marca").value("VW"))
				.andExpect(jsonPath("$.modelo").value("Gol"))
				.andExpect(jsonPath("$.clienteId").value(otroCliente.getId().intValue()));

		assertThat(vehiculoRepository.count()).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("vehiculosInvalidos")
	void actualizarConDatosInvalidosDevuelve400SinModificarVehiculo(
			String contenido, Set<String> camposConError) throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);

		var resultado = mockMvc.perform(put("/api/vehiculos/{id}", vehiculo.getId())
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
		comprobarVehiculoSinCambios(vehiculo.getId());
	}

	@Test
	void actualizarVehiculoInexistenteDevuelve404SinCrearOtro() throws Exception {
		long cantidadAnterior = vehiculoRepository.count();

		mockMvc.perform(put("/api/vehiculos/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("ABC123", cliente.getId())))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el vehículo con id 999999"));

		assertThat(vehiculoRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void actualizarConClienteInexistenteDevuelve404SinModificarVehiculo() throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);

		mockMvc.perform(put("/api/vehiculos/{id}", vehiculo.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("AB123CD", 999999L)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el cliente con id 999999"));

		comprobarVehiculoSinCambios(vehiculo.getId());
	}

	@Test
	void actualizarConPatenteDeOtroDevuelve409SinModificarVehiculo() throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);
		Cliente otroCliente = guardarCliente("Ana Díaz");
		guardarVehiculo("AB123CD", "VW", "Gol", otroCliente);

		mockMvc.perform(put("/api/vehiculos/{id}", vehiculo.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("AB123CD", otroCliente.getId())))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("Ya existe un vehículo con la patente AB123CD"));

		comprobarVehiculoSinCambios(vehiculo.getId());
		assertThat(vehiculoRepository.count()).isEqualTo(2);
	}

	@Test
	void actualizarConIdNoNumericoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(put("/api/vehiculos/abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido("ABC123", cliente.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));
	}

	@Test
	void actualizarConJsonMalFormadoDevuelve400SinModificarVehiculo() throws Exception {
		Vehiculo vehiculo = guardarVehiculo("ABC123", "Fiat", "Cronos", cliente);

		mockMvc.perform(put("/api/vehiculos/{id}", vehiculo.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"patente\":\"AB123CD\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarVehiculoSinCambios(vehiculo.getId());
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

	private Vehiculo guardarVehiculo(String patente, String marca, String modelo, Cliente propietario) {
		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente(patente);
		vehiculo.setMarca(marca);
		vehiculo.setModelo(modelo);
		vehiculo.setCliente(propietario);
		return vehiculoRepository.saveAndFlush(vehiculo);
	}

	private void comprobarVehiculoSinCambios(Long id) {
		assertThat(vehiculoRepository.findById(id)).hasValueSatisfying(vehiculo -> {
			assertThat(vehiculo.getPatente()).isEqualTo("ABC123");
			assertThat(vehiculo.getMarca()).isEqualTo("Fiat");
			assertThat(vehiculo.getModelo()).isEqualTo("Cronos");
			assertThat(vehiculo.getClienteId()).isEqualTo(cliente.getId());
		});
	}

	private String contenidoValido(String patente, Long clienteId) {
		return """
				{"patente":"%s","marca":"Fiat","modelo":"Cronos","clienteId":%d}
				""".formatted(patente, clienteId);
	}

}
