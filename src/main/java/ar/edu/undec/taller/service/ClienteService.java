package ar.edu.undec.taller.service;

import ar.edu.undec.taller.repository.ClienteRepository;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

	private final ClienteRepository clienteRepository;

	public ClienteService(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

}
