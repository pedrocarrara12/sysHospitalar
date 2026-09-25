package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.client.AtendimentoClient;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoServiceResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ContemObjetoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ServicoIndisponivelException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

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

        AtendimentoServiceResponse atendimentoServiceResponse = executarChamadaRemota(
                () -> atendimentoClient.cadastrar(request)
        );
        return converterParaResponse(atendimentoServiceResponse);
    }

    public AtendimentoResponse atualizar(Long id, AtendimentoRequest request) {
        validarId(id);
        validarRequest(request);
        validarPacienteEMedico(request);

        AtendimentoServiceResponse atendimentoSalvo = executarChamadaRemota(
                () -> atendimentoClient.atualizar(id, request)
        );
        return converterParaResponse(atendimentoSalvo);
    }

    public void remover(Long id) {
        validarId(id);
        executarChamadaRemota(() -> atendimentoClient.remover(id));
    }

    public AtendimentoResponse buscarPorId(Long id) {
        validarId(id);

        AtendimentoServiceResponse atendimentoServiceResponse = executarChamadaRemota(
                () -> atendimentoClient.buscarPorId(id)
        );
        return converterParaResponse(atendimentoServiceResponse);
    }

    public List<AtendimentoResponse> buscarTodos() {
        return converterListaParaResponse(executarChamadaRemota(atendimentoClient::buscarTodos));
    }

    public List<AtendimentoResponse> filtrarPorStatus(StatusAtendimento status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do atendimento e obrigatorio.");
        }

        return converterListaParaResponse(executarChamadaRemota(
                () -> atendimentoClient.filtrarPorStatus(status)
        ));
    }

    public List<AtendimentoResponse> filtrarPorTipo(TipoAtendimento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de atendimento e obrigatorio.");
        }

        return converterListaParaResponse(executarChamadaRemota(
                () -> atendimentoClient.filtrarPorTipo(tipo)
        ));
    }

    public List<AtendimentoResponse> listarOrdenadoPorDataHora() {
        return converterListaParaResponse(executarChamadaRemota(
                atendimentoClient::listarOrdenadoPorDataHora
        ));
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

    private <T> T executarChamadaRemota(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (FeignException.NotFound exception) {
            throw new ObjetoNaoEncontradoException("Atendimento nao encontrado");
        } catch (FeignException.BadRequest exception) {
            throw new IllegalArgumentException("Dados de atendimento invalidos");
        } catch (FeignException.Conflict exception) {
            throw new ContemObjetoException("Conflito ao processar atendimento");
        } catch (RetryableException exception) {
            throw new ServicoIndisponivelException(
                    "Servico de atendimentos temporariamente indisponivel",
                    exception
            );
        } catch (FeignException exception) {
            throw new ServicoIndisponivelException(
                    "Servico de atendimentos temporariamente indisponivel",
                    exception
            );
        }
    }

    private void executarChamadaRemota(Runnable chamada) {
        executarChamadaRemota(() -> {
            chamada.run();
            return null;
        });
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
