package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto;

import java.time.LocalDateTime;

public record ImportacaoPacienteInicioResponse(
        long executionId,
        String jobName,
        String status,
        String arquivo,
        LocalDateTime iniciadoEm
) {
}
