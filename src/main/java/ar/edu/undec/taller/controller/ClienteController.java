package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.service.ClienteService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClienteController {

	private final ClienteService clienteService;

	public ClienteController(ClienteService clienteService) {
		this.clienteService = clienteService;
	}

}
