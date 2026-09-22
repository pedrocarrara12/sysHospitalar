package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;

import java.time.LocalDateTime;

public record AtendimentoResponse(
        Long id,
        LocalDateTime dataHoraAtendimento,
        TipoAtendimento tipoAtendimento,
        StatusAtendimento statusAtendimento,
        Long pacienteId,
        String pacienteNome,
        Long medicoId,
        String medicoNome
) {
}
