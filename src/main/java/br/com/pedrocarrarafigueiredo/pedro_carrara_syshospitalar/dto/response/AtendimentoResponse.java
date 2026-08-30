package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;

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
