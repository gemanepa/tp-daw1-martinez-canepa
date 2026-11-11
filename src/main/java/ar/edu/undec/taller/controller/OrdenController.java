package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.service.OrdenService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenService ordenService;

	public OrdenController(OrdenService ordenService) {
		this.ordenService = ordenService;
	}

	@PostMapping
	public ResponseEntity<Orden> crear(@Valid @RequestBody OrdenSolicitud solicitud) {
		Orden creada = ordenService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/ordenes/" + creada.getId()))
				.body(creada);
	}

}
