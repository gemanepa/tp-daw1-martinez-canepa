package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.service.RepuestoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repuestos")
public class RepuestoController {

	private final RepuestoService repuestoService;

	public RepuestoController(RepuestoService repuestoService) {
		this.repuestoService = repuestoService;
	}

	@GetMapping
	public List<Repuesto> listar() {
		return repuestoService.listar();
	}

	@PostMapping
	public ResponseEntity<Repuesto> crear(@Valid @RequestBody Repuesto repuesto) {
		Repuesto creado = repuestoService.crear(repuesto);
		return ResponseEntity.created(URI.create("/api/repuestos/" + creado.getId()))
				.body(creado);
	}

}
