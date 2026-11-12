package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.dto.ConsumoRepuestoSolicitud;
import ar.edu.undec.taller.dto.OrdenActualizacionSolicitud;
import ar.edu.undec.taller.dto.OrdenEstadoSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.service.ConsumoRepuestoService;
import ar.edu.undec.taller.service.OrdenService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

	private final OrdenService ordenService;
	private final ConsumoRepuestoService consumoRepuestoService;

	public OrdenController(OrdenService ordenService, ConsumoRepuestoService consumoRepuestoService) {
		this.ordenService = ordenService;
		this.consumoRepuestoService = consumoRepuestoService;
	}

	@GetMapping
	public List<Orden> listar(@RequestParam(required = false) EstadoOrden estado) {
		return ordenService.listar(estado);
	}

	@PostMapping
	public ResponseEntity<Orden> crear(@Valid @RequestBody OrdenSolicitud solicitud) {
		Orden creada = ordenService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/ordenes/" + creada.getId()))
				.body(creada);
	}

	@PutMapping("/{id}")
	public Orden actualizar(@PathVariable Long id, @Valid @RequestBody OrdenActualizacionSolicitud solicitud) {
		return ordenService.actualizar(id, solicitud);
	}

	@PatchMapping("/{id}/estado")
	public Orden cambiarEstado(@PathVariable Long id, @Valid @RequestBody OrdenEstadoSolicitud solicitud) {
		return ordenService.cambiarEstado(id, solicitud.estado());
	}

	@PostMapping("/{id}/repuestos")
	public ResponseEntity<ConsumoRepuesto> registrarRepuesto(@PathVariable Long id,
			@Valid @RequestBody ConsumoRepuestoSolicitud solicitud) {
		ConsumoRepuesto creado = consumoRepuestoService.registrar(id, solicitud);
		return ResponseEntity.created(URI.create("/api/ordenes/" + id + "/repuestos/" + creado.getId()))
				.body(creado);
	}

}
