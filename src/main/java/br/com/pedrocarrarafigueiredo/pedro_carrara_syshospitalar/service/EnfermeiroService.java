package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.EnfermeiroRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnfermeiroService {
    private final EnfermeiroRepository enfermeiroRepository;

    public EnfermeiroService(EnfermeiroRepository enfermeiroRepository) {
        this.enfermeiroRepository = enfermeiroRepository;
    }

    public Enfermeiro cadastrar(Enfermeiro enfermeiro) {
        if (enfermeiro == null) {
            throw new IllegalArgumentException("Enfermeiro nao pode ser nulo");
        }

        return enfermeiroRepository.save(enfermeiro);
    }

    public Enfermeiro atualizar(Long id, Enfermeiro enfermeiro) {
        if (enfermeiro == null) {
            throw new IllegalArgumentException("Enfermeiro nao pode ser nulo");
        }

        buscarPorId(id);
        enfermeiro.setId(id);
        return enfermeiroRepository.save(enfermeiro);
    }

    public void remover(Long id) {
        buscarPorId(id);
        enfermeiroRepository.deleteById(id);
    }

    public Enfermeiro buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id nao pode ser nulo");
        }

        return enfermeiroRepository.findById(id)
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Enfermeiro nao encontrado"));
    }

    public List<Enfermeiro> buscarTodos() {
        return enfermeiroRepository.findAll();
    }

    public List<Enfermeiro> filtrarPorSetor(String setor) {
        if (setor == null || setor.isBlank()) {
            throw new IllegalArgumentException("Setor e obrigatorio.");
        }

        return enfermeiroRepository.findBySetorIgnoreCase(setor);
    }

    public List<Enfermeiro> listarAtivos() {
        return enfermeiroRepository.findByAtivoTrue();
    }
}
