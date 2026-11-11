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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrdenEstadoControllerTests {

	private final MockMvc mockMvc;
	private final OrdenRepository ordenRepository;
	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;
	private final JsonMapper jsonMapper;
	private Vehiculo vehiculo;

	@Autowired
	OrdenEstadoControllerTests(MockMvc mockMvc, OrdenRepository ordenRepository,
			VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.ordenRepository = ordenRepository;
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.jsonMapper = jsonMapper;
	}

	@BeforeEach
	void crearVehiculoConCliente() {
		Cliente cliente = new Cliente();
		cliente.setNombre("Carlos Ruiz");
		cliente.setTelefono("3825123456");
		clienteRepository.saveAndFlush(cliente);
		vehiculo = new Vehiculo();
		vehiculo.setPatente("ABC123");
		vehiculo.setMarca("Fiat");
		vehiculo.setModelo("Cronos");
		vehiculo.setCliente(cliente);
		vehiculoRepository.saveAndFlush(vehiculo);
	}

	@ParameterizedTest
	@CsvSource({
			"INGRESADO, EN_REPARACION, fechaInicioReparacion",
			"INGRESADO, CANCELADO, fechaCancelacion",
			"EN_REPARACION, TERMINADO, fechaTerminacion",
			"TERMINADO, ENTREGADO, fechaEntrega"
	})
	void transicionValidaDevuelve200YRegistraSoloLaFechaCorrespondiente(
			EstadoOrden origen, EstadoOrden destino, String campoFecha) throws Exception {
		Orden orden = guardarOrden(origen);
		var anterior = jsonMapper.readTree(jsonMapper.writeValueAsString(orden));
		LocalDateTime antesDelCambio = LocalDateTime.now();

		var respuesta = mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"" + destino + "\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$.id").value(orden.getId().intValue()))
				.andExpect(jsonPath("$.vehiculoId").value(vehiculo.getId().intValue()))
				.andExpect(jsonPath("$.descripcion").value("Revisión previa"))
				.andExpect(jsonPath("$.costoFinal").value(45000.75))
				.andExpect(jsonPath("$.estado").value(destino.name()))
				.andExpect(jsonPath("$." + campoFecha).isString())
				.andReturn().getResponse();

		var actual = jsonMapper.readTree(respuesta.getContentAsString());
		assertThat(LocalDateTime.parse(actual.get(campoFecha).stringValue()))
				.isBetween(antesDelCambio, LocalDateTime.now());
		for (String campo : List.of("fechaIngreso", "fechaInicioReparacion", "fechaTerminacion",
				"fechaEntrega", "fechaCancelacion")) {
			if (!campo.equals(campoFecha)) {
				assertThat(actual.get(campo)).isEqualTo(anterior.get(campo));
			}
		}
	}

	@ParameterizedTest
	@CsvSource({
			"INGRESADO, TERMINADO",
			"INGRESADO, ENTREGADO",
			"INGRESADO, INGRESADO",
			"EN_REPARACION, INGRESADO",
			"EN_REPARACION, CANCELADO",
			"TERMINADO, CANCELADO",
			"ENTREGADO, EN_REPARACION",
			"CANCELADO, EN_REPARACION"
	})
	void transicionNoPermitidaDevuelve409SinCambiarEstadoNiFechas(
			EstadoOrden origen, EstadoOrden destino) throws Exception {
		Orden orden = guardarOrden(origen);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"" + destino + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("No se puede pasar de " + origen + " a " + destino));

		comprobarSinCambios(orden.getId(), anterior);
	}

	@Test
	void entregaSinCostoDevuelve409SinCambiarEstadoNiFechas() throws Exception {
		Orden orden = guardarOrden(EstadoOrden.TERMINADO);
		orden.setCostoFinal(null);
		ordenRepository.saveAndFlush(orden);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"ENTREGADO\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflicto de negocio"))
				.andExpect(jsonPath("$.detail").value("No se puede entregar una orden sin costo final"));

		comprobarSinCambios(orden.getId(), anterior);
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"estado\":null}"})
	void estadoAusenteDevuelve400ConErroresSinModificarOrden(String contenido) throws Exception {
		Orden orden = guardarOrden(EstadoOrden.INGRESADO);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(contenido))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Datos inválidos"))
				.andExpect(jsonPath("$.detail").value("La petición tiene errores"))
				.andExpect(jsonPath("$.errores.estado").value("El estado es obligatorio"));

		comprobarSinCambios(orden.getId(), anterior);
	}

	@Test
	void estadoInvalidoDevuelve400SinModificarOrden() throws Exception {
		Orden orden = guardarOrden(EstadoOrden.INGRESADO);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"DESCONOCIDO\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarSinCambios(orden.getId(), anterior);
	}

	@Test
	void ordenInexistenteDevuelve404() throws Exception {
		mockMvc.perform(patch("/api/ordenes/999999/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"EN_REPARACION\"}"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso no encontrado"))
				.andExpect(jsonPath("$.detail").value("No existe la orden con id 999999"));

		assertThat(ordenRepository.count()).isZero();
	}

	@Test
	void idNoNumericoDevuelve400SinModificarOrden() throws Exception {
		Orden orden = guardarOrden(EstadoOrden.INGRESADO);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/abc/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"EN_REPARACION\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Parámetro inválido"))
				.andExpect(jsonPath("$.detail").value("El parámetro id tiene un valor inválido"));

		comprobarSinCambios(orden.getId(), anterior);
	}

	@Test
	void jsonMalFormadoDevuelve400SinModificarOrden() throws Exception {
		Orden orden = guardarOrden(EstadoOrden.EN_REPARACION);
		String anterior = jsonMapper.writeValueAsString(orden);

		mockMvc.perform(patch("/api/ordenes/{id}/estado", orden.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"TERMINADO\""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("JSON inválido"))
				.andExpect(jsonPath("$.detail").value("El JSON enviado no es válido"));

		comprobarSinCambios(orden.getId(), anterior);
	}

	private Orden guardarOrden(EstadoOrden estado) {
		LocalDateTime ingreso = LocalDateTime.of(2026, 11, 10, 10, 0);
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(estado);
		orden.setFechaIngreso(ingreso);
		orden.setCostoFinal(new BigDecimal("45000.75"));
		if (estado == EstadoOrden.EN_REPARACION || estado == EstadoOrden.TERMINADO || estado == EstadoOrden.ENTREGADO) {
			orden.setFechaInicioReparacion(ingreso.plusHours(1));
		}
		if (estado == EstadoOrden.TERMINADO || estado == EstadoOrden.ENTREGADO) {
			orden.setFechaTerminacion(ingreso.plusHours(2));
		}
		if (estado == EstadoOrden.ENTREGADO) {
			orden.setFechaEntrega(ingreso.plusHours(3));
		}
		if (estado == EstadoOrden.CANCELADO) {
			orden.setFechaCancelacion(ingreso.plusHours(1));
		}
		return ordenRepository.saveAndFlush(orden);
	}

	private void comprobarSinCambios(Long id, String anterior) {
		Orden orden = ordenRepository.findById(id).orElseThrow();
		assertThat(jsonMapper.readTree(jsonMapper.writeValueAsString(orden)))
				.isEqualTo(jsonMapper.readTree(anterior));
		assertThat(ordenRepository.count()).isEqualTo(1);
	}

}
