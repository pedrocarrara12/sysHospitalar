package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PacienteService {
    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente cadastrar(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente nao pode ser nulo");
        }

        return pacienteRepository.save(paciente);
    }

    public Paciente atualizar(Long id, Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente nao pode ser nulo");
        }

        buscarPorId(id);
        paciente.setId(id);
        return pacienteRepository.save(paciente);
    }

    public void remover(Long id) {
        buscarPorId(id);
        pacienteRepository.deleteById(id);
    }

    public Paciente buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id nao pode ser nulo");
        }

        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Paciente nao encontrado"));
    }

    public List<Paciente> buscarTodos() {
        return pacienteRepository.findAll();
    }

    public List<Paciente> filtrarPorSexo(char sexo) {

        if (!isSexoValido(sexo)) {
            throw new IllegalArgumentException("Sexo invalido. Informe M ou F.");
        }

        char sexoNormalizado = Character.toUpperCase(sexo);

        return pacienteRepository.findBySexo(sexoNormalizado);
    }

    public List<Paciente> listarOrdenadoPorNome() {
        return pacienteRepository.findAllByOrderByNomeAsc();
    }

    private boolean isSexoValido(char sexo) {
        char sexoNormalizado = Character.toUpperCase(sexo);
        return sexoNormalizado == 'M' || sexoNormalizado == 'F';
    }

}
