package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.client.AtendimentoClient;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoServiceResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.service.AtendimentoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.produtor.AtendimentoCriadoProducer;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceNotificacaoTest {

    @Mock
    private AtendimentoClient atendimentoClient;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private AtendimentoCriadoProducer producer;

    @Test
    void publicaEventoSomenteDepoisDaConfirmacaoRemota() {
        AtendimentoRequest request = request();
        Paciente paciente = paciente();
        Medico medico = medico();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(atendimentoClient.cadastrar(request)).thenReturn(response());
        AtendimentoService service = service();

        service.cadastrar(request);

        InOrder ordem = inOrder(atendimentoClient, producer);
        ordem.verify(atendimentoClient).cadastrar(request);
        ArgumentCaptor<AtendimentoCriadoEvento> evento = ArgumentCaptor.forClass(AtendimentoCriadoEvento.class);
        ordem.verify(producer).publicar(evento.capture());
        assertThat(evento.getValue().atendimentoId()).isEqualTo(42L);
        assertThat(evento.getValue().pacienteNome()).isEqualTo("Paciente Teste");
        assertThat(evento.getValue().pacienteEmail()).isEqualTo("paciente@email.com");
        assertThat(evento.getValue().tipoEvento()).isEqualTo(AtendimentoCriadoEvento.TIPO);
    }

    @Test
    void naoPublicaQuandoValidacaoFalha() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cadastrar(request()))
                .hasMessageContaining("Paciente nao encontrado");

        verify(atendimentoClient, never()).cadastrar(request());
        verify(producer, never()).publicar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void naoPublicaQuandoServicoRemotoNaoConfirmaCriacao() {
        AtendimentoRequest request = request();
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente()));
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico()));
        when(atendimentoClient.cadastrar(request)).thenReturn(null);

        assertThatThrownBy(() -> service().cadastrar(request))
                .hasMessageContaining("Resposta do servico de atendimento");

        verify(producer, never()).publicar(org.mockito.ArgumentMatchers.any());
    }

    private AtendimentoService service() {
        return new AtendimentoService(atendimentoClient, pacienteRepository, medicoRepository, producer);
    }

    private AtendimentoRequest request() {
        return new AtendimentoRequest(
                LocalDateTime.of(2026, 10, 10, 14, 30),
                TipoAtendimento.URGENCIA,
                StatusAtendimento.ANDAMENTO,
                1L,
                1L
        );
    }

    private AtendimentoServiceResponse response() {
        return new AtendimentoServiceResponse(
                42L,
                LocalDateTime.of(2026, 10, 10, 14, 30),
                TipoAtendimento.URGENCIA,
                StatusAtendimento.ANDAMENTO,
                1L,
                1L
        );
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente(
                "Paciente Teste", "12345678901", LocalDate.of(1990, 1, 1),
                'F', "65999990000", "paciente@email.com", true);
        paciente.setId(1L);
        return paciente;
    }

    private Medico medico() {
        Medico medico = new Medico(
                "Medico Teste", 40, "10987654321", "medico@email.com",
                true, "CRM-1", "Clinica Geral");
        medico.setId(1L);
        return medico;
    }
}
