package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AtendimentoRequest(
        @NotNull(message = "Data e hora do atendimento sao obrigatorias.")
        LocalDateTime dataHoraAtendimento,

        @NotNull(message = "Tipo de atendimento e obrigatorio.")
        TipoAtendimento tipoAtendimento,

        @NotNull(message = "Status do atendimento e obrigatorio.")
        StatusAtendimento statusAtendimento,

        @NotNull(message = "Id do paciente e obrigatorio.")
        @Positive(message = "Id do paciente deve ser positivo.")
        Long pacienteId,

        @NotNull(message = "Id do medico e obrigatorio.")
        @Positive(message = "Id do medico deve ser positivo.")
        Long medicoId
) {
}
