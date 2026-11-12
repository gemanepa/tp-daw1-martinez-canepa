package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.ConsumoRepuestoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.repository.ConsumoRepuestoRepository;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.RepuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumoRepuestoService {

	private final OrdenRepository ordenRepository;
	private final RepuestoRepository repuestoRepository;
	private final ConsumoRepuestoRepository consumoRepuestoRepository;

	public ConsumoRepuestoService(OrdenRepository ordenRepository, RepuestoRepository repuestoRepository,
			ConsumoRepuestoRepository consumoRepuestoRepository) {
		this.ordenRepository = ordenRepository;
		this.repuestoRepository = repuestoRepository;
		this.consumoRepuestoRepository = consumoRepuestoRepository;
	}

	@Transactional
	public ConsumoRepuesto registrar(Long ordenId, ConsumoRepuestoSolicitud solicitud) {
		Orden orden = ordenRepository.findById(ordenId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden con id " + ordenId));
		Repuesto repuesto = repuestoRepository.findById(solicitud.repuestoId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe el repuesto con id " + solicitud.repuestoId()));

		if (repuesto.getStock() < solicitud.cantidad()) {
			throw new OperacionNoPermitidaException(
					"No hay stock suficiente del repuesto con id " + solicitud.repuestoId());
		}

		repuesto.setStock(repuesto.getStock() - solicitud.cantidad());
		repuestoRepository.save(repuesto);
		ConsumoRepuesto consumo = new ConsumoRepuesto();
		consumo.setOrden(orden);
		consumo.setRepuesto(repuesto);
		consumo.setCantidad(solicitud.cantidad());
		orden.getConsumos().add(consumo);
		return consumoRepuestoRepository.save(consumo);
	}

}
