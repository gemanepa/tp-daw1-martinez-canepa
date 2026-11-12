package ar.edu.undec.taller.exception;

public class CostoFinalInvalidoException extends RuntimeException {

	public CostoFinalInvalidoException() {
		super("El costo final no puede ser menor que el costo de repuestos");
	}

}
