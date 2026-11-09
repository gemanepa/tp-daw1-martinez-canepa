package ar.edu.undec.taller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail jsonInvalido() {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "El JSON enviado no es válido");
		problema.setTitle("JSON inválido");
		return problema;
	}

}
