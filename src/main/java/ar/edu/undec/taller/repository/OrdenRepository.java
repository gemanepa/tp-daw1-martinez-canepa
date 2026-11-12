package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

	List<Orden> findByEstado(EstadoOrden estado);

	List<Orden> findByVehiculo_PatenteOrderByFechaIngresoDesc(String patente);

	List<Orden> findByEstadoAndFechaEntregaGreaterThanEqualAndFechaEntregaLessThan(
			EstadoOrden estado, LocalDateTime desde, LocalDateTime hasta);

	boolean existsByVehiculo_Id(Long vehiculoId);

	boolean existsByVehiculo_IdAndEstadoNotIn(Long vehiculoId, List<EstadoOrden> estados);

}
