package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.EstadoOrden;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenFlujoControllerTests {

	private final MockMvc mockMvc;
	private final JsonMapper jsonMapper;
	private final JdbcTemplate jdbcTemplate;
	private Long clienteId;
	private Long vehiculoId;

	@Autowired
	OrdenFlujoControllerTests(MockMvc mockMvc, JsonMapper jsonMapper, JdbcTemplate jdbcTemplate) {
		this.mockMvc = mockMvc;
		this.jsonMapper = jsonMapper;
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	void crearClienteYVehiculoPorApi() throws Exception {
		var cliente = mockMvc.perform(post("/api/clientes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Carlos Ruiz\",\"telefono\":\"3825123456\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
		clienteId = jsonMapper.readTree(cliente.getContentAsString()).get("id").longValue();

		var vehiculo = mockMvc.perform(post("/api/vehiculos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"patente":"ABC123","marca":"Fiat","modelo":"Cronos","clienteId":%d}
						""".formatted(clienteId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
		vehiculoId = jsonMapper.readTree(vehiculo.getContentAsString()).get("id").longValue();
	}

	@AfterEach
	void eliminarSoloLosDatosDeEsteFlujo() {
		if (vehiculoId != null) {
			jdbcTemplate.update("delete from orden where vehiculo_id = ?", vehiculoId);
			jdbcTemplate.update("delete from vehiculo where id = ?", vehiculoId);
		}
		if (clienteId != null) {
			jdbcTemplate.update("delete from cliente where id = ?", clienteId);
		}
	}

	@Test
	void entregarPermiteCrearOtraOrdenYSigueRechazandoDuplicadosAbiertos() throws Exception {
		long primeraId = crearOrden();
		comprobarRechazoMientrasHayUnaOrdenAbierta();
		cambiarEstado(primeraId, EstadoOrden.EN_REPARACION);
		comprobarRechazoMientrasHayUnaOrdenAbierta();
		cambiarEstado(primeraId, EstadoOrden.TERMINADO);
		comprobarRechazoMientrasHayUnaOrdenAbierta();

		mockMvc.perform(put("/api/ordenes/{id}", primeraId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"descripcion\":\"Cambio de aceite y filtro\",\"costoFinal\":45000.75}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("TERMINADO"));
		cambiarEstado(primeraId, EstadoOrden.ENTREGADO);

		long nuevaId = crearOrden();
		assertThat(nuevaId).isNotEqualTo(primeraId);
		comprobarRechazoMientrasHayUnaOrdenAbierta();
		comprobarAmbasOrdenes(primeraId, EstadoOrden.ENTREGADO, nuevaId);
	}

	@Test
	void cancelarPermiteCrearOtraOrdenYSigueRechazandoDuplicadosAbiertos() throws Exception {
		long primeraId = crearOrden();
		comprobarRechazoMientrasHayUnaOrdenAbierta();
		cambiarEstado(primeraId, EstadoOrden.CANCELADO);

		long nuevaId = crearOrden();
		assertThat(nuevaId).isNotEqualTo(primeraId);
		comprobarRechazoMientrasHayUnaOrdenAbierta();
		comprobarAmbasOrdenes(primeraId, EstadoOrden.CANCELADO, nuevaId);
	}

	private long crearOrden() throws Exception {
		var respuesta = mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"vehiculoId\":%d,\"descripcion\":\"Cambio de aceite\"}".formatted(vehiculoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vehiculoId").value(vehiculoId.intValue()))
				.andExpect(jsonPath("$.estado").value("INGRESADO"))
				.andReturn().getResponse();
		long id = jsonMapper.readTree(respuesta.getContentAsString()).get("id").longValue();
		assertThat(respuesta.getHeader("Location")).isEqualTo("/api/ordenes/" + id);
		return id;
	}

	private void cambiarEstado(long id, EstadoOrden estado) throws Exception {
		mockMvc.perform(patch("/api/ordenes/{id}/estado", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"" + estado + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value((int) id))
				.andExpect(jsonPath("$.estado").value(estado.name()));
	}

	private void comprobarRechazoMientrasHayUnaOrdenAbierta() throws Exception {
		mockMvc.perform(post("/api/ordenes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"vehiculoId\":%d,\"descripcion\":\"Intento duplicado\"}".formatted(vehiculoId)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("El vehículo ya tiene una orden abierta"));
	}

	private void comprobarAmbasOrdenes(long primeraId, EstadoOrden estadoFinal, long nuevaId) throws Exception {
		String jsonEsperado = """
				[
				  {"id":%d,"vehiculoId":%d,"estado":"%s"},
				  {"id":%d,"vehiculoId":%d,"estado":"INGRESADO"}
				]
				""".formatted(primeraId, vehiculoId, estadoFinal.name(), nuevaId, vehiculoId);
		mockMvc.perform(get("/api/ordenes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(content().json(jsonEsperado));
	}

}
