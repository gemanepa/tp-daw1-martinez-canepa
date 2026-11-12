package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.Cliente;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.ClienteRepository;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculoService {

	private final VehiculoRepository vehiculoRepository;
	private final ClienteRepository clienteRepository;
	private final OrdenRepository ordenRepository;

	public VehiculoService(VehiculoRepository vehiculoRepository, ClienteRepository clienteRepository,
			OrdenRepository ordenRepository) {
		this.vehiculoRepository = vehiculoRepository;
		this.clienteRepository = clienteRepository;
		this.ordenRepository = ordenRepository;
	}

	public List<Vehiculo> listar() {
		return vehiculoRepository.findAll();
	}

	public List<Orden> historial(String patente) {
		return ordenRepository.findByVehiculo_PatenteOrderByFechaIngresoDesc(patente);
	}

	public Vehiculo buscarPorId(Long id) {
		return vehiculoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el vehículo con id " + id));
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

	@Transactional
	public Vehiculo actualizar(Long id, VehiculoSolicitud solicitud) {
		Vehiculo vehiculo = buscarPorId(id);
		Cliente cliente = clienteRepository.findById(solicitud.clienteId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el cliente con id " + solicitud.clienteId()));

		if (vehiculoRepository.existsByPatenteAndIdNot(solicitud.patente(), id)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un vehículo con la patente " + solicitud.patente());
		}

		vehiculo.setPatente(solicitud.patente());
		vehiculo.setMarca(solicitud.marca());
		vehiculo.setModelo(solicitud.modelo());
		vehiculo.setCliente(cliente);
		return vehiculoRepository.save(vehiculo);
	}

	@Transactional
	public void eliminar(Long id) {
		Vehiculo vehiculo = buscarPorId(id);
		if (ordenRepository.existsByVehiculo_Id(id)) {
			throw new OperacionNoPermitidaException("El vehículo tiene órdenes asociadas");
		}
		vehiculoRepository.delete(vehiculo);
	}

}
