package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime localDateTime
        ,int status, String error, String mensagem, String path) {
}
