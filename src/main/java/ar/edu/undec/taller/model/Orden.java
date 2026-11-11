package ar.edu.undec.taller.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Orden {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonIgnore
	@ManyToOne(optional = false)
	@JoinColumn(name = "vehiculo_id", nullable = false)
	private Vehiculo vehiculo;

	@Column(nullable = false)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoOrden estado;

	@Column(nullable = false)
	private LocalDateTime fechaIngreso;

	private BigDecimal costoFinal;

	public Long getId() {
		return id;
	}

	public Vehiculo getVehiculo() {
		return vehiculo;
	}

	public void setVehiculo(Vehiculo vehiculo) {
		this.vehiculo = vehiculo;
	}

	public Long getVehiculoId() {
		return vehiculo.getId();
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public EstadoOrden getEstado() {
		return estado;
	}

	public void setEstado(EstadoOrden estado) {
		this.estado = estado;
	}

	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public BigDecimal getCostoFinal() {
		return costoFinal;
	}

	public void setCostoFinal(BigDecimal costoFinal) {
		this.costoFinal = costoFinal;
	}

}
