package ar.edu.undec.taller.controller;

import ar.edu.undec.taller.dto.VehiculoSolicitud;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.service.VehiculoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

	@PostMapping
	public ResponseEntity<Vehiculo> crear(@Valid @RequestBody VehiculoSolicitud solicitud) {
		Vehiculo creado = vehiculoService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/vehiculos/" + creado.getId()))
				.body(creado);
	}

}
