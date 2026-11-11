package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

	List<Orden> findByEstado(EstadoOrden estado);

	boolean existsByVehiculo_IdAndEstadoNotIn(Long vehiculoId, List<EstadoOrden> estados);

}
