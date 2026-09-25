package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.client.AtendimentoClient;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoServiceResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtendimentoService {

    private final AtendimentoClient atendimentoClient;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;

    public AtendimentoService(AtendimentoClient atendimentoClient, PacienteRepository pacienteRepository,
        MedicoRepository medicoRepository) {
        this.atendimentoClient = atendimentoClient;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
    }

    public AtendimentoResponse cadastrar(AtendimentoRequest request) {
        validarRequest(request);
        validarPacienteEMedico(request);

        AtendimentoServiceResponse atendimentoServiceResponse = atendimentoClient.cadastrar(request);
        return converterParaResponse(atendimentoServiceResponse);
    }

    public AtendimentoResponse atualizar(Long id, AtendimentoRequest request) {
        validarId(id);
        validarRequest(request);
        validarPacienteEMedico(request);

        AtendimentoServiceResponse atendimentoSalvo = atendimentoClient.atualizar(id, request);
        return converterParaResponse(atendimentoSalvo);
    }

    public void remover(Long id) {
        validarId(id);
        atendimentoClient.remover(id);
    }

    public AtendimentoResponse buscarPorId(Long id) {
        validarId(id);

        AtendimentoServiceResponse atendimentoServiceResponse = atendimentoClient.buscarPorId(id);
        return converterParaResponse(atendimentoServiceResponse);
    }

    public List<AtendimentoResponse> buscarTodos() {
        return converterListaParaResponse(atendimentoClient.buscarTodos());
    }

    public List<AtendimentoResponse> filtrarPorStatus(StatusAtendimento status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do atendimento e obrigatorio.");
        }

        return converterListaParaResponse(atendimentoClient.filtrarPorStatus(status));
    }

    public List<AtendimentoResponse> filtrarPorTipo(TipoAtendimento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de atendimento e obrigatorio.");
        }

        return converterListaParaResponse(atendimentoClient.filtrarPorTipo(tipo));
    }

    public List<AtendimentoResponse> listarOrdenadoPorDataHora() {
        return converterListaParaResponse(atendimentoClient.listarOrdenadoPorDataHora());
    }

    private void validarRequest(AtendimentoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Atendimento nao pode ser nulo");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id do atendimento deve ser positivo");
        }
    }

    private void validarPacienteEMedico(AtendimentoRequest request) {
        pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Paciente nao encontrado"));
        medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Medico nao encontrado"));
    }

    private AtendimentoResponse converterParaResponse(AtendimentoServiceResponse atendimentoServiceResponse) {
        if (atendimentoServiceResponse == null) {
            throw new IllegalArgumentException("Resposta do servico de atendimento nao pode ser nula");
        }

        Paciente paciente = pacienteRepository.findById(atendimentoServiceResponse.pacienteId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Paciente nao encontrado"));
        Medico medico = medicoRepository.findById(atendimentoServiceResponse.medicoId())
                .orElseThrow(() -> new ObjetoNaoEncontradoException("Medico nao encontrado"));

        return new AtendimentoResponse(
                atendimentoServiceResponse.id(),
                atendimentoServiceResponse.dataHoraAtendimento(),
                atendimentoServiceResponse.tipoAtendimento(),
                atendimentoServiceResponse.statusAtendimento(),
                atendimentoServiceResponse.pacienteId(),
                paciente.getNome(),
                atendimentoServiceResponse.medicoId(),
                medico.getNome()
        );
    }

    private List<AtendimentoResponse> converterListaParaResponse(
            List<AtendimentoServiceResponse> atendimentosServiceResponse) {
        if (atendimentosServiceResponse == null) {
            throw new IllegalArgumentException("Lista de atendimentos do servico nao pode ser nula");
        }

        return atendimentosServiceResponse.stream()
                .map(this::converterParaResponse)
                .toList();
    }
}
