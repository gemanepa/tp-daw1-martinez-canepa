package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.VehiculoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

	private final VehiculoService vehiculoService;

	public VehiculoController(VehiculoService vehiculoService) {
		this.vehiculoService = vehiculoService;
	}

	@GetMapping
	public List<Vehiculo> listar() {
		return vehiculoService.listar();
	}

	@GetMapping("/{id}")
	public Vehiculo obtener(@PathVariable Long id) {
		return vehiculoService.buscarPorId(id);
	}

	@GetMapping("/patente/{patente}/historial")
	public List<Orden> historial(@PathVariable String patente) {
		return vehiculoService.historial(patente);
	}

	@PostMapping
	public ResponseEntity<Vehiculo> crear(@Valid @RequestBody VehiculoSolicitud solicitud) {
		Vehiculo creado = vehiculoService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/vehiculos/" + creado.getId()))
				.body(creado);
	}

	@PutMapping("/{id}")
	public Vehiculo actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoSolicitud solicitud) {
		return vehiculoService.actualizar(id, solicitud);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		vehiculoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}
