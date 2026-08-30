package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.MedicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService {
    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public Medico cadastrar(Medico medico) {
        if (medico == null) {
            throw new IllegalArgumentException("Medico nao pode ser nulo");
        }

        return medicoRepository.save(medico);
    }

    public Medico atualizar(Long id, Medico medico) {
        if (medico == null) {
            throw new IllegalArgumentException("Medico nao pode ser nulo");
        }

        buscarPorId(id);
        medico.setId(id);
        return medicoRepository.save(medico);
    }

    public void remover(Long id) {
        buscarPorId(id);
        medicoRepository.deleteById(id);
    }

    public Medico buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id nao pode ser nulo");
        }

        return medicoRepository.findById(id)
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Medico nao encontrado"));
    }

    public List<Medico> buscarTodos() {
        return medicoRepository.findAll();
    }

    public List<Medico> filtrarPorEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            throw new IllegalArgumentException("Especialidade e obrigatoria.");
        }

        return medicoRepository.findByEspecialidadeIgnoreCase(especialidade);
    }

    public List<Medico> listarAtivos() {
        return medicoRepository.findByAtivoTrue();
    }
}
