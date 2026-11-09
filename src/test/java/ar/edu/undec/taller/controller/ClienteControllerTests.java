package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
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

}
