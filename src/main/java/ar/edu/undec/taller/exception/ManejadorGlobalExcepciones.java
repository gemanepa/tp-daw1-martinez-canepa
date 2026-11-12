package ar.edu.undec.taller.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail jsonInvalido() {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "El JSON enviado no es válido");
		problema.setTitle("JSON inválido");
		return problema;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail datosInvalidos(MethodArgumentNotValidException excepcion) {
		Map<String, String> errores = new HashMap<>();
		excepcion.getBindingResult().getFieldErrors().forEach(error ->
				errores.put(error.getField(), error.getDefaultMessage()));

		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "La petición tiene errores");
		problema.setTitle("Datos inválidos");
		problema.setProperty("errores", errores);
		return problema;
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException excepcion) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, excepcion.getMessage());
		problema.setTitle("Recurso no encontrado");
		return problema;
	}

	@ExceptionHandler(CostoFinalInvalidoException.class)
	public ProblemDetail costoFinalInvalido(CostoFinalInvalidoException excepcion) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, excepcion.getMessage());
		problema.setTitle("Datos inválidos");
		problema.setProperty("errores", Map.of("costoFinal", excepcion.getMessage()));
		return problema;
	}

	@ExceptionHandler(OperacionNoPermitidaException.class)
	public ProblemDetail operacionNoPermitida(OperacionNoPermitidaException excepcion) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, excepcion.getMessage());
		problema.setTitle("Conflicto de negocio");
		return problema;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException excepcion) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"El parámetro " + excepcion.getName() + " tiene un valor inválido");
		problema.setTitle("Parámetro inválido");
		return problema;
	}

}
