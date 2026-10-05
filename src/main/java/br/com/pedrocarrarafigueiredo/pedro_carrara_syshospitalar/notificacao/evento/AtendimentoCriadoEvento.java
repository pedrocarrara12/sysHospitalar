package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AtendimentoCriadoEvento(
        UUID eventoId,
        String tipoEvento,
        Long atendimentoId,
        String pacienteNome,
        String pacienteEmail,
        LocalDateTime dataHoraAtendimento
) {
    public static final String TIPO = "ATENDIMENTO_CRIADO";

    public AtendimentoCriadoEvento {
        if (eventoId == null || atendimentoId == null || atendimentoId <= 0
                || pacienteNome == null || pacienteNome.isBlank()
                || pacienteEmail == null || pacienteEmail.isBlank()
                || dataHoraAtendimento == null) {
            throw new IllegalArgumentException("Dados do evento de atendimento criado sao obrigatorios");
        }
        tipoEvento = TIPO;
    }
}
