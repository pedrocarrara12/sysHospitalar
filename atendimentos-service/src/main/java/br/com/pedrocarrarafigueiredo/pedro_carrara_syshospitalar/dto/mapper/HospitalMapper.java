package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoResponse;

public final class HospitalMapper {

    private HospitalMapper() {
    }

    public static Atendimento toEntity(AtendimentoRequest request) {
        return new Atendimento(
                request.dataHoraAtendimento(),
                request.tipoAtendimento(),
                request.statusAtendimento(),
                request.pacienteId(),
                request.medicoId()
        );
    }

    public static AtendimentoResponse toResponse(Atendimento atendimento) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getDataHoraAtendimento(),
                atendimento.getTipoAtendimento(),
                atendimento.getStatusAtendimento(),
                atendimento.getPacienteId(),
                atendimento.getMedicoId()
        );
    }
}
