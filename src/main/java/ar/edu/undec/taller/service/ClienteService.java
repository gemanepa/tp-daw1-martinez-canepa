package ar.edu.undec.taller.service;

import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

	private final ClienteRepository clienteRepository;
	private final VehiculoRepository vehiculoRepository;

	public ClienteService(ClienteRepository clienteRepository, VehiculoRepository vehiculoRepository) {
		this.clienteRepository = clienteRepository;
		this.vehiculoRepository = vehiculoRepository;
	}

	public Cliente crear(Cliente cliente) {
		return clienteRepository.save(cliente);
	}

	public List<Vehiculo> listarVehiculos(Long id) {
		if (!clienteRepository.existsById(id)) {
			throw new RecursoNoEncontradoException("No existe el cliente con id " + id);
		}
		return vehiculoRepository.findByCliente_Id(id);
	}

}
