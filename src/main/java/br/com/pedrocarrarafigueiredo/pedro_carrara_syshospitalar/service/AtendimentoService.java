package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper.HospitalMapper;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.AtendimentoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public AtendimentoService(AtendimentoRepository atendimentoRepository, PacienteRepository pacienteRepository,
        MedicoRepository medicoRepository) {
        this.atendimentoRepository = atendimentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    public Atendimento cadastrar(AtendimentoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Atendimento nao pode ser nulo");
        }

        Atendimento atendimento = criarAtendimento(request);
        return atendimentoRepository.save(atendimento);
    }

    public Atendimento atualizar(Long id, AtendimentoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Atendimento nao pode ser nulo");
        }

        buscarPorId(id);
        Atendimento atendimento = criarAtendimento(request);
        atendimento.setId(id);
        return atendimentoRepository.save(atendimento);
    }

    public void remover(Long id) {
        buscarPorId(id);
        atendimentoRepository.deleteById(id);
    }

    public Atendimento buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id nao pode ser nulo");
        }

        return atendimentoRepository.findById(id)
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Atendimento nao encontrado"));
    }

    public List<Atendimento> buscarTodos() {
        return atendimentoRepository.findAll();
    }

    public List<Atendimento> filtrarPorStatus(StatusAtendimento status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do atendimento e obrigatorio.");
        }

        return atendimentoRepository.findByStatusAtendimento(status);
    }

    public List<Atendimento> filtrarPorTipo(TipoAtendimento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de atendimento e obrigatorio.");
        }

        return atendimentoRepository.findByTipoAtendimento(tipo);
    }

    public List<Atendimento> listarOrdenadoPorDataHora() {
        return atendimentoRepository.findAllByOrderByDataHoraAtendimentoAsc();
    }

    private Atendimento criarAtendimento(AtendimentoRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Paciente nao encontrado"));
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Medico nao encontrado"));

        return HospitalMapper.toEntity(request, paciente, medico);
    }
}
