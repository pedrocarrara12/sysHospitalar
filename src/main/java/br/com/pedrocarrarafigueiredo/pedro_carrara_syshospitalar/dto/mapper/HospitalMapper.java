package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.dto.request.EnfermeiroRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.dto.response.EnfermeiroResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.dto.request.MedicoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.dto.response.MedicoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.dto.request.PacienteRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.dto.response.PacienteResponse;

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

}
