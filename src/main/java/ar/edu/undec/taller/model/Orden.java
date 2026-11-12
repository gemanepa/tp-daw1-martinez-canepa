package ar.edu.undec.taller.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

	@JsonIgnore
	@OneToMany(mappedBy = "orden")
	private List<ConsumoRepuesto> consumos = new ArrayList<>();

	private LocalDateTime fechaInicioReparacion;
	private LocalDateTime fechaTerminacion;
	private LocalDateTime fechaEntrega;
	private LocalDateTime fechaCancelacion;

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

	public List<ConsumoRepuesto> getConsumos() {
		return consumos;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public BigDecimal getCostoRepuestos() {
		if (consumos.isEmpty()) {
			return null;
		}
		BigDecimal total = BigDecimal.ZERO;
		for (ConsumoRepuesto consumo : consumos) {
			total = total.add(consumo.getRepuesto().getPrecio()
					.multiply(BigDecimal.valueOf(consumo.getCantidad())));
		}
		return total;
	}

	public LocalDateTime getFechaInicioReparacion() {
		return fechaInicioReparacion;
	}

	public void setFechaInicioReparacion(LocalDateTime fechaInicioReparacion) {
		this.fechaInicioReparacion = fechaInicioReparacion;
	}

	public LocalDateTime getFechaTerminacion() {
		return fechaTerminacion;
	}

	public void setFechaTerminacion(LocalDateTime fechaTerminacion) {
		this.fechaTerminacion = fechaTerminacion;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Double getHorasEnReparacion() {
		if (fechaInicioReparacion == null || fechaTerminacion == null) {
			return null;
		}
		return Duration.between(fechaInicioReparacion, fechaTerminacion).toSeconds() / 3600.0;
	}

	public LocalDateTime getFechaEntrega() {
		return fechaEntrega;
	}

	public void setFechaEntrega(LocalDateTime fechaEntrega) {
		this.fechaEntrega = fechaEntrega;
	}

	public LocalDateTime getFechaCancelacion() {
		return fechaCancelacion;
	}

	public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
		this.fechaCancelacion = fechaCancelacion;
	}

}
