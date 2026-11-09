package ar.edu.undec.taller.repository;

import ar.edu.undec.taller.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
