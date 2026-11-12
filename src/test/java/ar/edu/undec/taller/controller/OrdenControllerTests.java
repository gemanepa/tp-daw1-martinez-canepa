package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.math.BigDecimal;
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
	void listarSinOrdenesDevuelve200YArrayVacio() throws Exception {
		mockMvc.perform(get("/api/ordenes"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void listarSinFiltroDevuelveTodasLasOrdenesConSusIdentificadores() throws Exception {
		Orden primera = guardarOrden(vehiculo, EstadoOrden.INGRESADO);
		Vehiculo otroVehiculo = guardarVehiculo("AB123CD");
		Orden segunda = guardarOrden(otroVehiculo, EstadoOrden.EN_REPARACION);
		String jsonEsperado = """
				[
				  {"id":%d,"vehiculoId":%d,"descripcion":"Revisión previa","estado":"INGRESADO"},
				  {"id":%d,"vehiculoId":%d,"descripcion":"Revisión previa","estado":"EN_REPARACION"}
				]
				""".formatted(primera.getId(), vehiculo.getId(), segunda.getId(), otroVehiculo.getId());

		mockMvc.perform(get("/api/ordenes"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(content().json(jsonEsperado));
	}

	@ParameterizedTest
	@EnumSource(EstadoOrden.class)
	void filtrarPorCualquierEstadoValidoDevuelveSoloLasOrdenesDeEseEstado(EstadoOrden estado) throws Exception {
		Orden esperada = guardarOrden(vehiculo, estado);
		Vehiculo otroVehiculo = guardarVehiculo("AB123CD");
		EstadoOrden otroEstado = estado == EstadoOrden.INGRESADO ? EstadoOrden.CANCELADO : EstadoOrden.INGRESADO;
		guardarOrden(otroVehiculo, otroEstado);

		mockMvc.perform(get("/api/ordenes").param("estado", estado.name()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(esperada.getId().intValue()))
				.andExpect(jsonPath("$[0].vehiculoId").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$[0].estado").value(estado.name()));
	}

	@Test
	void filtroSinCoincidenciasDevuelve200YArrayVacio() throws Exception {
		guardarOrden(vehiculo, EstadoOrden.INGRESADO);

		mockMvc.perform(get("/api/ordenes").param("estado", "EN_REPARACION"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void estadoInvalidoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/ordenes").param("estado", "INEXISTENTE"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro estado tiene un valor inválido"));
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

	@Test
	void actualizarDescripcionYCostoDevuelve200YConservaVehiculoEstadoYFecha() throws Exception {
		Orden orden = guardarOrdenParaActualizar();
		LocalDateTime fechaAnterior = orden.getFechaIngreso();

		var respuesta = mockMvc.perform(put("/api/ordenes/{id}", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoActualizacionValido()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$.id").value(orden.getId().intValue()))
				.andExpect(jsonPath("$.descripcion").value("Cambio de aceite y filtro"))
				.andExpect(jsonPath("$.costoFinal").value(45000.75))
				.andExpect(jsonPath("$.vehiculoId").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.estado").value("EN_REPARACION"))
				.andReturn().getResponse();

		var json = jsonMapper.readTree(respuesta.getContentAsString());
		assertThat(LocalDateTime.parse(json.get("fechaIngreso").stringValue())).isEqualTo(fechaAnterior);
		assertThat(ordenRepository.count()).isEqualTo(1);
	}

	@Test
	void actualizarDescripcionPermiteDejarCostoFinalPendiente() throws Exception {
		Orden orden = guardarOrden(vehiculo, EstadoOrden.INGRESADO);

		mockMvc.perform(put("/api/ordenes/{id}", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\":\"Revisar frenos\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(orden.getId().intValue()))
				.andExpect(jsonPath("$.descripcion").value("Revisar frenos"))
				.andExpect(jsonPath("$.costoFinal").doesNotExist())
				.andExpect(jsonPath("$.estado").value("INGRESADO"));
	}

	@ParameterizedTest
	@MethodSource("actualizacionesInvalidas")
	void actualizarConDescripcionInvalidaDevuelve400SinModificarOrden(String contenido) throws Exception {
		Orden orden = guardarOrdenParaActualizar();

		mockMvc.perform(put("/api/ordenes/{id}", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"))
				.andExpect(jsonPath("$.errores.descripcion").value("La descripción es obligatoria"));

		comprobarOrdenSinCambios(orden.getId());
	}

	@Test
	void actualizarOrdenInexistenteDevuelve404SinCrearOtra() throws Exception {
		long cantidadAnterior = ordenRepository.count();

		mockMvc.perform(put("/api/ordenes/999999")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoActualizacionValido()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe la orden con id 999999"));

		assertThat(ordenRepository.count()).isEqualTo(cantidadAnterior);
	}

	@Test
	void actualizarConIdNoNumericoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(put("/api/ordenes/abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenidoActualizacionValido()))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));
	}

	@Test
	void actualizarConCostoNoNumericoDevuelve400SinModificarOrden() throws Exception {
		Orden orden = guardarOrdenParaActualizar();

		mockMvc.perform(put("/api/ordenes/{id}", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\":\"Cambio de aceite\",\"costoFinal\":\"abc\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarOrdenSinCambios(orden.getId());
	}

	@Test
	void actualizarConJsonMalFormadoDevuelve400SinModificarOrden() throws Exception {
		Orden orden = guardarOrdenParaActualizar();

		mockMvc.perform(put("/api/ordenes/{id}", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\":\"Cambio de aceite\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarOrdenSinCambios(orden.getId());
	}

	@Test
	void facturacionDevuelve200ConCantidadYTotalDelMesDeEntrega() throws Exception {
		guardarOrdenFacturacion(EstadoOrden.ENTREGADO, "2026-10-31T23:59:59", "1000.00");
		guardarOrdenFacturacion(EstadoOrden.ENTREGADO, "2026-11-01T00:00:00", "10000.50");
		guardarOrdenFacturacion(EstadoOrden.ENTREGADO, "2026-11-30T23:59:59", "45000.75");
		guardarOrdenFacturacion(EstadoOrden.ENTREGADO, "2026-12-01T00:00:00", "30000.00");
		guardarOrdenFacturacion(EstadoOrden.CANCELADO, null, "95000.00");
		guardarOrdenFacturacion(EstadoOrden.EN_REPARACION, null, "99000.00");

		mockMvc.perform(get("/api/ordenes/facturacion").param("mes", "2026-11"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$.cantidadOrdenes").value(2))
				.andExpect(jsonPath("$.totalFacturado").value(55001.25));
	}

	@Test
	void facturacionSinEntregasEnElMesDevuelve200ConCantidadYTotalEnCero() throws Exception {
		guardarOrdenFacturacion(EstadoOrden.ENTREGADO, "2026-10-31T23:59:59", "1000.00");

		mockMvc.perform(get("/api/ordenes/facturacion").param("mes", "2026-11"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.cantidadOrdenes").value(0))
				.andExpect(jsonPath("$.totalFacturado").value(0));
	}

	@Test
	void facturacionConMesInvalidoDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/ordenes/facturacion").param("mes", "2026-13"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro mes tiene un valor inválido"));
	}

	@Test
	void facturacionSinMesDevuelve400ConProblemDetail() throws Exception {
		mockMvc.perform(get("/api/ordenes/facturacion"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro obligatorio"))
				.andExpect(jsonPath("$.detail").value("El parámetro mes es obligatorio"));
	}

	private void guardarOrdenFacturacion(EstadoOrden estado, String fechaEntrega, String costoFinal) {
		Orden orden = guardarOrden(vehiculo, estado);
		orden.setFechaIngreso(LocalDateTime.of(2026, 10, 1, 10, 0));
		orden.setFechaEntrega(fechaEntrega == null ? null : LocalDateTime.parse(fechaEntrega));
		orden.setCostoFinal(new BigDecimal(costoFinal));
		ordenRepository.saveAndFlush(orden);
	}

	static Stream<String> actualizacionesInvalidas() {
		return Stream.of(
				"{\"costoFinal\":45000.75}",
				"{\"descripcion\":\"\",\"costoFinal\":45000.75}",
				"{\"descripcion\":null,\"costoFinal\":45000.75}");
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

	private Orden guardarOrden(Vehiculo vehiculo, EstadoOrden estado) {
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(estado);
		orden.setFechaIngreso(LocalDateTime.of(2026, 11, 10, 10, 0));
		return ordenRepository.saveAndFlush(orden);
	}

	private Orden guardarOrdenParaActualizar() {
		Orden orden = guardarOrden(vehiculo, EstadoOrden.EN_REPARACION);
		orden.setCostoFinal(new BigDecimal("10000.50"));
		return ordenRepository.saveAndFlush(orden);
	}

	private void comprobarOrdenSinCambios(Long id) {
		assertThat(ordenRepository.findById(id)).hasValueSatisfying(orden -> {
			assertThat(orden.getDescripcion()).isEqualTo("Revisión previa");
			assertThat(orden.getCostoFinal()).isEqualByComparingTo("10000.50");
			assertThat(orden.getVehiculoId()).isEqualTo(vehiculo.getId());
			assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EN_REPARACION);
			assertThat(orden.getFechaIngreso()).isEqualTo(LocalDateTime.of(2026, 11, 10, 10, 0));
		});
	}

	private String contenidoActualizacionValido() {
		return """
				{"descripcion":"Cambio de aceite y filtro","costoFinal":45000.75}
				""";
	}

	private String contenidoValido(Long vehiculoId) {
		return """
				{"vehiculoId":%d,"descripcion":"Cambio de aceite"}
				""".formatted(vehiculoId);
	}

}
