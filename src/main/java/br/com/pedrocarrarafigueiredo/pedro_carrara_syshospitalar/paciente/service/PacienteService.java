package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.mapper.HospitalMapper;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ContemObjetoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.dto.request.PacienteRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PacienteService {
    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente cadastrar(PacienteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Paciente nao pode ser nulo");
        }

        validarUnicidade(request, null);

        return pacienteRepository.save(HospitalMapper.toEntity(request));
    }

    public Paciente atualizar(Long id, PacienteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Paciente nao pode ser nulo");
        }

        buscarPorId(id);
        validarUnicidade(request, id);
        Paciente paciente = HospitalMapper.toEntity(request);
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

    private void validarUnicidade(PacienteRequest request, Long idAtual) {
        boolean cpfEmUso = idAtual == null
                ? pacienteRepository.existsByCpf(request.cpf())
                : pacienteRepository.existsByCpfAndIdNot(request.cpf(), idAtual);
        if (cpfEmUso) {
            throw new ContemObjetoException("CPF ja cadastrado para outro paciente");
        }

        boolean emailEmUso = idAtual == null
                ? pacienteRepository.existsByEmailIgnoreCase(request.email())
                : pacienteRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), idAtual);
        if (emailEmUso) {
            throw new ContemObjetoException("E-mail ja cadastrado para outro paciente");
        }
    }

}
