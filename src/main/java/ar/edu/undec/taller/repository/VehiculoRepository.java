package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.Vehiculo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

	boolean existsByPatente(String patente);

	boolean existsByPatenteAndIdNot(String patente, Long id);

	List<Vehiculo> findByCliente_Id(Long clienteId);

}
