package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.service.ClienteService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

	private final ClienteService clienteService;

	public ClienteController(ClienteService clienteService) {
		this.clienteService = clienteService;
	}

	@PostMapping
	public ResponseEntity<Cliente> crear(@RequestBody Cliente cliente) {
		Cliente creado = clienteService.crear(cliente);
		return ResponseEntity.created(URI.create("/api/clientes/" + creado.getId()))
				.body(creado);
	}

}
