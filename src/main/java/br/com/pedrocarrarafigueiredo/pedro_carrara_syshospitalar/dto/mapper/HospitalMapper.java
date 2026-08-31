package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.EnfermeiroRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.MedicoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.PacienteRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.AtendimentoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.EnfermeiroResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.MedicoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response.PacienteResponse;

public final class HospitalMapper {

    private HospitalMapper() {
    }

    public static Paciente toEntity(PacienteRequest request) {
        return new Paciente(
                request.nome(),
                request.cpf(),
                request.dataNascimento(),
                request.sexo().charAt(0),
                request.telefone(),
                request.email(),
                request.ativo()
        );
    }

    public static Medico toEntity(MedicoRequest request) {
        return new Medico(
                request.nome(),
                request.idade(),
                request.cpf(),
                request.email(),
                request.ativo(),
                request.crm(),
                request.especialidade()
        );
    }

    public static Enfermeiro toEntity(EnfermeiroRequest request) {
        return new Enfermeiro(
                request.nome(),
                request.idade(),
                request.cpf(),
                request.email(),
                request.ativo(),
                request.coren(),
                request.setor()
        );
    }

    public static Atendimento toEntity(AtendimentoRequest request, Paciente paciente, Medico medico) {
        return new Atendimento(
                request.dataHoraAtendimento(),
                request.tipoAtendimento(),
                request.statusAtendimento(),
                paciente,
                medico
        );
    }

    public static PacienteResponse toResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getDataNascimento(),
                paciente.getSexo(),
                paciente.getTelefone(),
                paciente.getEmail(),
                paciente.isAtivo()
        );
    }

    public static MedicoResponse toResponse(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNome(),
                medico.getIdade(),
                medico.getCpf(),
                medico.getEmail(),
                medico.isAtivo(),
                medico.getCrm(),
                medico.getEspecialidade()
        );
    }

    public static EnfermeiroResponse toResponse(Enfermeiro enfermeiro) {
        return new EnfermeiroResponse(
                enfermeiro.getId(),
                enfermeiro.getNome(),
                enfermeiro.getIdade(),
                enfermeiro.getCpf(),
                enfermeiro.getEmail(),
                enfermeiro.isAtivo(),
                enfermeiro.getCoren(),
                enfermeiro.getSetor()
        );
    }

    public static AtendimentoResponse toResponse(Atendimento atendimento) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getDataHoraAtendimento(),
                atendimento.getTipoAtendimento(),
                atendimento.getStatusAtendimento(),
                atendimento.getPaciente().getId(),
                atendimento.getPaciente().getNome(),
                atendimento.getMedico().getId(),
                atendimento.getMedico().getNome()
        );
    }
}
