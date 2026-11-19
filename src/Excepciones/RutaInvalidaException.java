package Excepciones;

public class RutaInvalidaException extends Exception {
    public RutaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public RutaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}