package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculoService {

	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;

	public VehiculoService(VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository) {
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
	}

	public List<Vehiculo> listar() {
		return vehiculoRepository.findAll();
	}

	@Transactional
	public Vehiculo crear(VehiculoSolicitud solicitud) {
		Cliente cliente = clienteRepository.findById(solicitud.clienteId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el cliente con id " + solicitud.clienteId()));

		if (vehiculoRepository.existsByPatente(solicitud.patente())) {
			throw new OperacionNoPermitidaException(
					"Ya existe un vehículo con la patente " + solicitud.patente());
		}

		Vehiculo vehiculo = new Vehiculo();
		vehiculo.setPatente(solicitud.patente());
		vehiculo.setMarca(solicitud.marca());
		vehiculo.setModelo(solicitud.modelo());
		vehiculo.setCliente(cliente);
		return vehiculoRepository.save(vehiculo);
	}

}
