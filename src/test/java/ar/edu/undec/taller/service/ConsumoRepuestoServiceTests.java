package ar.edu.undec.taller.service;

import ar.edu.undec.taller.dto.ConsumoRepuestoSolicitud;
import ar.edu.undec.taller.exception.OperacionNoPermitidaException;
import ar.edu.undec.taller.model.ConsumoRepuesto;
import ar.edu.undec.taller.model.Orden;
import ar.edu.undec.taller.model.Repuesto;
import ar.edu.undec.taller.repository.ConsumoRepuestoRepository;
import ar.edu.undec.taller.repository.OrdenRepository;
import ar.edu.undec.taller.repository.RepuestoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
class ConsumoRepuestoServiceTests {

	@Mock
	private OrdenRepository ordenRepository;

	@Mock
	private RepuestoRepository repuestoRepository;

	@Mock
	private ConsumoRepuestoRepository consumoRepuestoRepository;

	@InjectMocks
	private ConsumoRepuestoService consumoRepuestoService;

	@ParameterizedTest
	@ValueSource(ints = {3, 10})
	void registrarConsumoDescuentaLaCantidadIncluidoTodoElStock(int cantidad) {
		Orden orden = new Orden();
		Repuesto repuesto = new Repuesto();
		repuesto.setStock(10);
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
		when(repuestoRepository.findById(2L)).thenReturn(Optional.of(repuesto));
		when(consumoRepuestoRepository.save(any(ConsumoRepuesto.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		ConsumoRepuesto consumo = consumoRepuestoService.registrar(1L,
				new ConsumoRepuestoSolicitud(2L, cantidad));

		assertThat(repuesto.getStock()).isEqualTo(10 - cantidad);
		assertThat(consumo.getCantidad()).isEqualTo(cantidad);
		assertThat(consumo.getOrden()).isSameAs(orden);
		assertThat(consumo.getRepuesto()).isSameAs(repuesto);
	}

	@Test
	void stockInsuficienteRechazaElConsumoSinDescontarNiGuardar() {
		Repuesto repuesto = new Repuesto();
		repuesto.setStock(2);
		when(ordenRepository.findById(1L)).thenReturn(Optional.of(new Orden()));
		when(repuestoRepository.findById(2L)).thenReturn(Optional.of(repuesto));

		assertThatThrownBy(() -> consumoRepuestoService.registrar(1L,
				new ConsumoRepuestoSolicitud(2L, 3)))
				.isInstanceOf(OperacionNoPermitidaException.class)
				.hasMessage("No hay stock suficiente del repuesto con id 2");
		assertThat(repuesto.getStock()).isEqualTo(2);
		verify(repuestoRepository, never()).save(any(Repuesto.class));
		verify(consumoRepuestoRepository, never()).save(any(ConsumoRepuesto.class));
	}

}
