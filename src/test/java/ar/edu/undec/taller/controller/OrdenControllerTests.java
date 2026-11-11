package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrdenControllerTests {

	private final MockMvc mockMvc;
	private final OrdenRepository ordenRepository;
	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;
	private final JsonMapper jsonMapper;
	private Cliente cliente;
	private Vehiculo vehiculo;

	@Autowired
	OrdenControllerTests(MockMvc mockMvc, OrdenRepository ordenRepository,
			VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.ordenRepository = ordenRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.jsonMapper = jsonMapper;
	}

	@BeforeEach
	void crearVehiculoConCliente() {
		cliente = new Cliente();
		cliente.setNombre("Carlos Ruiz");
		cliente.setTelefono("3825123456");
		clienteRepository.saveAndFlush(cliente);
		vehiculo = guardarVehiculo("ABC123");
	}

	@Test
	void crearOrdenDevuelve201ConEstadoFechaVehiculoIdYLocation() throws Exception {
		LocalDateTime antes = LocalDateTime.now();

		var respuesta = mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(vehiculo.getId())))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.vehiculoId").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.descripcion").value("Cambio de aceite"))
				.andExpect(jsonPath("$.estado").value("INGRESADO"))
				.andExpect(jsonPath("$.fechaIngreso").isString())
				.andExpect(jsonPath("$.vehiculo").doesNotExist())
				.andReturn().getResponse();

		var json = jsonMapper.readTree(respuesta.getContentAsString());
		long id = json.get("id").longValue();
		assertThat(respuesta.getHeader("Location")).isEqualTo("/api/ordenes/" + id);
		assertThat(LocalDateTime.parse(json.get("fechaIngreso").stringValue()))
				.isBetween(antes, LocalDateTime.now());
		assertThat(ordenRepository.findById(id)).isPresent();
	}

	@ParameterizedTest
	@MethodSource("ordenesInvalidas")
	void datosInvalidosDevuelven400ConErroresSinGuardarOrden(
			String contenido, Set<String> camposConError) throws Exception {
		long cantidadAnterior = ordenRepository.count();

		var resultado = mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido.formatted(vehiculo.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"));

		for (String campo : camposConError) {
			resultado.andExpect(jsonPath("$.errores." + campo).isNotEmpty());
		}
		assertThat(ordenRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void vehiculoInexistenteDevuelve404SinGuardarOrden() throws Exception {
		long cantidadAnterior = ordenRepository.count();

		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(999999L)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe el vehículo con id 999999"));

		assertThat(ordenRepository.count()).isEqualTo(cantidadAnterior);
	}

	@ParameterizedTest
	@EnumSource(value = EstadoOrden.class, names = {"INGRESADO", "EN_REPARACION", "TERMINADO"})
	void ordenAbiertaDevuelve409SinGuardarOtra(EstadoOrden estado) throws Exception {
		guardarOrden(vehiculo, estado);
		long cantidadAnterior = ordenRepository.count();

		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(vehiculo.getId())))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("El vehículo ya tiene una orden abierta"));

		assertThat(ordenRepository.count()).isEqualTo(cantidadAnterior);
	}

	@ParameterizedTest
	@EnumSource(value = EstadoOrden.class, names = {"ENTREGADO", "CANCELADO"})
	void ordenCerradaPermiteCrearUnaNuevaEnEstadoIngresado(EstadoOrden estado) throws Exception {
		guardarOrden(vehiculo, estado);

		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(vehiculo.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("INGRESADO"));

		assertThat(ordenRepository.count()).isEqualTo(2);
	}

	@Test
	void ordenAbiertaDeOtroVehiculoNoImpideCrearLaOrden() throws Exception {
		Vehiculo otroVehiculo = guardarVehiculo("AB123CD");
		guardarOrden(otroVehiculo, EstadoOrden.INGRESADO);

		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoValido(vehiculo.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vehiculoId").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.estado").value("INGRESADO"));

		assertThat(ordenRepository.count()).isEqualTo(2);
	}

	@Test
	void jsonMalFormadoDevuelve400SinGuardarOrden() throws Exception {
		long cantidadAnterior = ordenRepository.count();

		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\":\"Cambio de aceite\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		assertThat(ordenRepository.count()).isEqualTo(cantidadAnterior);
	}

	static Stream<Arguments> ordenesInvalidas() {
		return Stream.of(
				Arguments.of("{}", Set.of("vehiculoId", "descripcion")),
				Arguments.of("{\"vehiculoId\":null,\"descripcion\":\"Cambio de aceite\"}",
						Set.of("vehiculoId")),
				Arguments.of("{\"descripcion\":\"Cambio de aceite\"}", Set.of("vehiculoId")),
				Arguments.of("{\"vehiculoId\":%d}", Set.of("descripcion")),
				Arguments.of("{\"vehiculoId\":%d,\"descripcion\":\"\"}", Set.of("descripcion")),
				Arguments.of("{\"vehiculoId\":%d,\"descripcion\":null}", Set.of("descripcion")));
	}

	private Vehiculo guardarVehiculo(String patente) {
		Vehiculo nuevo = new Vehiculo();
		nuevo.setPatente(patente);
		nuevo.setMarca("Fiat");
		nuevo.setModelo("Cronos");
		nuevo.setCliente(cliente);
		return vehiculoRepository.saveAndFlush(nuevo);
	}

	private void guardarOrden(Vehiculo vehiculo, EstadoOrden estado) {
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(estado);
		orden.setFechaIngreso(LocalDateTime.of(2026, 11, 10, 10, 0));
		ordenRepository.saveAndFlush(orden);
	}

	private String contenidoValido(Long vehiculoId) {
		return """
				{"vehiculoId":%d,"descripcion":"Cambio de aceite"}
				""".formatted(vehiculoId);
	}

}
