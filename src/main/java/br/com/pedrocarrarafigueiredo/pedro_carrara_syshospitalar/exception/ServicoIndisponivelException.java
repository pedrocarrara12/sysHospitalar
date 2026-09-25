package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception;

public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String message) {
        super(message);
    }

    public ServicoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
