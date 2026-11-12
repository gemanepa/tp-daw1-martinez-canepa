package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.OrdenActualizacionSolicitud;
import ar.edu.undec.taller.dto.OrdenSolicitud;
import ar.edu.undec.taller.exception.CostoFinalInvalidoException;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.exception.RecursoNoEncontradoException;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.EstadoOrden;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.model.Vehiculo;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.VehiculoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTests {

	@Mock
	private OrdenRepository ordenRepository;

	@Mock
	private VehiculoRepository vehiculoRepository;

	@InjectMocks
	private OrdenService ordenService;

	@Test
	void asignarEstadoIngresadoYFechaDesdeElSistema() {
		Vehiculo vehiculo = new Vehiculo();
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
		when(ordenRepository.save(any(Orden.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		LocalDateTime antes = LocalDateTime.now();

		Orden orden = ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite"));

		assertThat(orden.getEstado()).isEqualTo(EstadoOrden.INGRESADO);
		assertThat(orden.getFechaIngreso()).isBetween(antes, LocalDateTime.now());
		assertThat(orden.getDescripcion()).isEqualTo("Cambio de aceite");
		assertThat(orden.getVehiculo()).isSameAs(vehiculo);
	}

	@Test
	void rechazarVehiculoInexistenteSinGuardarOrden() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite")))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessage("No existe el vehículo con id 1");
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@Test
	void rechazarOtraOrdenAbiertaSinGuardarOrden() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(new Vehiculo()));
		when(ordenRepository.existsByVehiculo_IdAndEstadoNotIn(1L,
				List.of(EstadoOrden.ENTREGADO, EstadoOrden.CANCELADO))).thenReturn(true);

		assertThatThrownBy(() -> ordenService.crear(new OrdenSolicitud(1L, "Cambio de aceite")))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("El vehículo ya tiene una orden abierta");
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@Test
	void actualizarSoloDescripcionYCostoConservaRelacionEstadoYFecha() {
		Vehiculo vehiculo = new Vehiculo();
		LocalDateTime fecha = LocalDateTime.of(2026, 11, 10, 10, 0);
		Orden orden = new Orden();
		orden.setVehiculo(vehiculo);
		orden.setDescripcion("Revisión previa");
		orden.setEstado(EstadoOrden.EN_REPARACION);
		orden.setFechaIngreso(fecha);
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
		when(ordenRepository.save(orden)).thenReturn(orden);

		Orden actualizada = ordenService.actualizar(1L,
				new OrdenActualizacionSolicitud("Cambio de aceite y filtro", new BigDecimal("45000.75")));

		assertThat(actualizada.getDescripcion()).isEqualTo("Cambio de aceite y filtro");
		assertThat(actualizada.getCostoFinal()).isEqualByComparingTo("45000.75");
		assertThat(actualizada.getVehiculo()).isSameAs(vehiculo);
		assertThat(actualizada.getEstado()).isEqualTo(EstadoOrden.EN_REPARACION);
		assertThat(actualizada.getFechaIngreso()).isEqualTo(fecha);
	}

	@ParameterizedTest
	@CsvSource({
			"INGRESADO, TERMINADO",
			"INGRESADO, ENTREGADO",
			"INGRESADO, INGRESADO",
			"EN_REPARACION, INGRESADO",
			"EN_REPARACION, CANCELADO",
			"TERMINADO, CANCELADO",
			"ENTREGADO, EN_REPARACION",
			"CANCELADO, EN_REPARACION"
	})
	void rechazarTransicionNoPermitidaSinCambiarEstadoNiFechas(EstadoOrden origen, EstadoOrden destino) {
		Orden orden = ordenEnEstado(origen);
		Orden anterior = ordenEnEstado(origen);
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

		assertThatThrownBy(() -> ordenService.cambiarEstado(1L, destino))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("No se puede pasar de " + origen + " a " + destino);
		assertThat(orden).usingRecursiveComparison().isEqualTo(anterior);
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@Test
	void rechazarEntregaSinCostoSinCambiarEstadoNiFechas() {
		Orden orden = ordenEnEstado(EstadoOrden.TERMINADO);
		orden.setCostoFinal(null);
		Orden anterior = ordenEnEstado(EstadoOrden.TERMINADO);
		anterior.setCostoFinal(null);
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

		assertThatThrownBy(() -> ordenService.cambiarEstado(1L, EstadoOrden.ENTREGADO))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("No se puede entregar una orden sin costo final");
		assertThat(orden).usingRecursiveComparison().isEqualTo(anterior);
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@ParameterizedTest
	@ValueSource(strings = {"29753.00", "35000.50"})
	void sumarPrecioPorCantidadYPermitirCostoFinalIgualOMayor(String costoFinal) {
		Orden orden = ordenConConsumos();
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
		when(ordenRepository.save(orden)).thenReturn(orden);

		Orden actualizada = ordenService.actualizar(1L,
				new OrdenActualizacionSolicitud("Reparación completa", new BigDecimal(costoFinal)));

		assertThat(actualizada.getCostoRepuestos()).isEqualByComparingTo("29753.00");
		assertThat(actualizada.getCostoFinal()).isEqualByComparingTo(costoFinal);
		assertThat(actualizada.getDescripcion()).isEqualTo("Reparación completa");
	}

	@Test
	void costoFinalInferiorRechazaLaActualizacionSinModificarLaOrden() {
		Orden orden = ordenConConsumos();
		Orden anterior = ordenConConsumos();
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

		assertThatThrownBy(() -> ordenService.actualizar(1L,
				new OrdenActualizacionSolicitud("Cambio rechazado", new BigDecimal("29752.99"))))
				.isInstanceOf(CostoFinalInvalidoException.class)
				.hasMessage("El costo final no puede ser menor que el costo de repuestos");
		assertThat(orden).usingRecursiveComparison().isEqualTo(anterior);
		verify(ordenRepository, never()).save(any(Orden.class));
	}

	@Test
	void facturacionCuentaEntregasYSumaCostosFinalesConCentavos() {
		Orden primera = ordenEnEstado(EstadoOrden.ENTREGADO);
		primera.setCostoFinal(new BigDecimal("10000.50"));
		Orden segunda = ordenEnEstado(EstadoOrden.ENTREGADO);
		segunda.setCostoFinal(new BigDecimal("45000.75"));
		when(ordenRepository.findByEstadoAndFechaEntregaGreaterThanEqualAndFechaEntregaLessThan(
				EstadoOrden.ENTREGADO, LocalDateTime.of(2026, 11, 1, 0, 0),
				LocalDateTime.of(2026, 12, 1, 0, 0))).thenReturn(List.of(primera, segunda));

		var facturacion = ordenService.facturacion(YearMonth.of(2026, 11));

		assertThat(facturacion.cantidadOrdenes()).isEqualTo(2);
		assertThat(facturacion.totalFacturado()).isEqualByComparingTo("55001.25");
	}

	@Test
	void facturacionSinEntregasDevuelveCantidadYTotalEnCero() {
		when(ordenRepository.findByEstadoAndFechaEntregaGreaterThanEqualAndFechaEntregaLessThan(
				EstadoOrden.ENTREGADO, LocalDateTime.of(2026, 11, 1, 0, 0),
				LocalDateTime.of(2026, 12, 1, 0, 0))).thenReturn(List.of());

		var facturacion = ordenService.facturacion(YearMonth.of(2026, 11));

		assertThat(facturacion.cantidadOrdenes()).isZero();
		assertThat(facturacion.totalFacturado()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	private Orden ordenConConsumos() {
		Orden orden = ordenEnEstado(EstadoOrden.EN_REPARACION);
		Repuesto filtro = new Repuesto();
		filtro.setPrecio(new BigDecimal("8500.75"));
		ConsumoRepuesto primero = new ConsumoRepuesto();
		primero.setOrden(orden);
		primero.setRepuesto(filtro);
		primero.setCantidad(2);
		Repuesto bujia = new Repuesto();
		bujia.setPrecio(new BigDecimal("4250.50"));
		ConsumoRepuesto segundo = new ConsumoRepuesto();
		segundo.setOrden(orden);
		segundo.setRepuesto(bujia);
		segundo.setCantidad(3);
		orden.getConsumos().addAll(List.of(primero, segundo));
		return orden;
	}

	private Orden ordenEnEstado(EstadoOrden estado) {
		LocalDateTime ingreso = LocalDateTime.of(2026, 11, 10, 10, 0);
		Orden orden = new Orden();
		orden.setVehiculo(new Vehiculo());
		orden.setDescripcion("Revisión previa");
		orden.setEstado(estado);
		orden.setFechaIngreso(ingreso);
		orden.setCostoFinal(new BigDecimal("45000.75"));
		if (estado == EstadoOrden.EN_REPARACION || estado == EstadoOrden.TERMINADO || estado == EstadoOrden.ENTREGADO) {
			orden.setFechaInicioReparacion(ingreso.plusHours(1));
		}
		if (estado == EstadoOrden.TERMINADO || estado == EstadoOrden.ENTREGADO) {
			orden.setFechaTerminacion(ingreso.plusHours(2));
		}
		if (estado == EstadoOrden.ENTREGADO) {
			orden.setFechaEntrega(ingreso.plusHours(3));
		}
		if (estado == EstadoOrden.CANCELADO) {
			orden.setFechaCancelacion(ingreso.plusHours(1));
		}
		return orden;
	}

}
