package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.OrdenActualizacionSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenService {

	private final OrdenRepository ordenRepository;
	private final VehiculoRepository vehiculoRepository;

	public OrdenService(OrdenRepository ordenRepository, VehiculoRepository vehiculoRepository) {
		this.ordenRepository = ordenRepository;
		this.vehiculoRepository = vehiculoRepository;
	}

	public List<Orden> listar(EstadoOrden estado) {
		if (estado == null) {
			return ordenRepository.findAll();
		}
		return ordenRepository.findByEstado(estado);
	}

	public Orden buscarPorId(Long id) {
		return ordenRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe la orden con id " + id));
	}

	@Transactional
	public Orden crear(OrdenSolicitud solicitud) {
		Vehiculo vehiculo = vehiculoRepository.findById(solicitud.vehiculoId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el vehículo con id " + solicitud.vehiculoId()));

		if (ordenRepository.existsByVehiculo_IdAndEstadoNotIn(solicitud.vehiculoId(),
				List.of(EstadoOrden.ENTREGADO, EstadoOrden.CANCELADO))) {
			throw new OperacionNoPermitidaException("El vehículo ya tiene una orden abierta");
		}

		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion(solicitud.descripcion());
		orden.setEstado(EstadoOrden.INGRESADO);
		orden.setFechaIngreso(LocalDateTime.now());
		return ordenRepository.save(orden);
	}

	@Transactional
	public Orden actualizar(Long id, OrdenActualizacionSolicitud solicitud) {
		Orden orden = buscarPorId(id);
		orden.setDescripcion(solicitud.descripcion());
		orden.setCostoFinal(solicitud.costoFinal());
		return ordenRepository.save(orden);
	}

}
