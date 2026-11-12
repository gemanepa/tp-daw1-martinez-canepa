package ar.edu.undec.taller.service;

import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.repository.RepuestoRepository;
import org.springframework.stereotype.Service;

@Service
public class RepuestoService {

	private final RepuestoRepository repuestoRepository;

	public RepuestoService(RepuestoRepository repuestoRepository) {
		this.repuestoRepository = repuestoRepository;
	}

	public Repuesto crear(Repuesto repuesto) {
		return repuestoRepository.save(repuesto);
	}

}
