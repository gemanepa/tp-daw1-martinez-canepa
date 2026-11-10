package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

	boolean existsByPatente(String patente);

}
