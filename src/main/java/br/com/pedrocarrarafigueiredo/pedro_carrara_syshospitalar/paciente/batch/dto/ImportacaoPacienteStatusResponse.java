package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto;

import java.time.LocalDateTime;

public record ImportacaoPacienteStatusResponse(
        long executionId,
        String status,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm,
        long lidos,
        long gravados,
        long filtrados,
        long ignorados,
        String resumo
) {
}
