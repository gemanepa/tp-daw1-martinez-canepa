package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.ClienteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	public ResponseEntity<Cliente> crear(@Valid @RequestBody Cliente cliente) {
		Cliente creado = clienteService.crear(cliente);
		return ResponseEntity.created(URI.create("/api/clientes/" + creado.getId()))
				.body(creado);
	}

	@GetMapping("/{id}/vehiculos")
	public List<Vehiculo> listarVehiculos(@PathVariable Long id) {
		return clienteService.listarVehiculos(id);
	}

}
