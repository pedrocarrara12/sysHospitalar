package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.produtor.AtendimentoCriadoProducer;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.service.EmailAtendimentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificacaoTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void enviaConfirmacaoDiscretaEmTextoSimples() {
        EmailAtendimentoService service = new EmailAtendimentoService(mailSender, "hospital@example.com");
        AtendimentoCriadoEvento evento = evento();

        service.enviarConfirmacao(evento);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertThat(mensagem.getFrom()).isEqualTo("hospital@example.com");
        assertThat(mensagem.getTo()).containsExactly("paciente@example.com");
        assertThat(mensagem.getSubject()).isEqualTo("Confirmação de atendimento #42");
        assertThat(mensagem.getText())
                .isEqualTo("Olá, Ana Silva.\nSeu atendimento #42 foi registrado para 10/10/2026 14:30.\n"
                        + "Esta é uma mensagem automática.")
                .doesNotContain("medico", "diagnostico", "AMBULATORIAL");
    }

    @Test
    void falhaDoBrokerNaoEscapaDoProdutor() {
        AtendimentoCriadoProducer producer = new AtendimentoCriadoProducer(
                rabbitTemplate, "atendimentos.exchange", "atendimento.criado");
        AtendimentoCriadoEvento evento = evento();
        doThrow(new AmqpConnectException(new RuntimeException("broker indisponivel")))
                .when(rabbitTemplate)
                .convertAndSend("atendimentos.exchange", "atendimento.criado", evento);

        assertThatCode(() -> producer.publicar(evento)).doesNotThrowAnyException();
    }

    private AtendimentoCriadoEvento evento() {
        return new AtendimentoCriadoEvento(
                UUID.fromString("f6ccde56-cf95-4d0c-b183-bc4968b89f50"),
                AtendimentoCriadoEvento.TIPO,
                42L,
                "Ana Silva",
                "paciente@example.com",
                LocalDateTime.of(2026, 10, 10, 14, 30)
        );
    }
}
