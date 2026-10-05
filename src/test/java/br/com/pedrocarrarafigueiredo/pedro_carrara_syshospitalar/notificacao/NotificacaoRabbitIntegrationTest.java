package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.produtor.AtendimentoCriadoProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "notificacoes.consumidor.ativo=true")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificacaoRabbitIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired
    private AtendimentoCriadoProducer producer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void limparFilas() {
        rabbitAdmin.purgeQueue("notificacao-email.atendimento-criado", false);
        rabbitAdmin.purgeQueue("notificacao-email.atendimento-criado.dlq", false);
        clearInvocations(mailSender);
    }

    @Test
    void mensagemPublicadaChegaAoConsumidor() {
        producer.publicar(evento(51L));

        verify(mailSender, timeout(5000)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void falhaSmtpEEncaminhadaParaDlqDepoisDasRetentativas() throws InterruptedException {
        doThrow(new MailSendException("SMTP indisponivel"))
                .when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        producer.publicar(evento(52L));

        verify(mailSender, timeout(10000).times(3)).send(any(org.springframework.mail.SimpleMailMessage.class));
        Message mensagemDlq = aguardarMensagemDlq();
        assertThat(mensagemDlq).isNotNull();
    }

    @Test
    void mensagemPermaneceNaFilaEnquantoConsumidorEstaParado() throws InterruptedException {
        listenerRegistry.stop();
        try {
            producer.publicar(evento(53L));
            assertThat(aguardarQuantidadeNaFila(1)).isTrue();
            verify(mailSender, org.mockito.Mockito.never())
                    .send(any(org.springframework.mail.SimpleMailMessage.class));
        } finally {
            listenerRegistry.start();
        }

        verify(mailSender, timeout(5000)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    private Message aguardarMensagemDlq() throws InterruptedException {
        for (int tentativa = 0; tentativa < 50; tentativa++) {
            Message mensagem = rabbitTemplate.receive("notificacao-email.atendimento-criado.dlq");
            if (mensagem != null) {
                return mensagem;
            }
            Thread.sleep(100);
        }
        return null;
    }

    private boolean aguardarQuantidadeNaFila(int quantidade) throws InterruptedException {
        for (int tentativa = 0; tentativa < 50; tentativa++) {
            var fila = rabbitAdmin.getQueueInfo("notificacao-email.atendimento-criado");
            if (fila != null && fila.getMessageCount() >= quantidade) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private AtendimentoCriadoEvento evento(long atendimentoId) {
        return new AtendimentoCriadoEvento(
                UUID.randomUUID(),
                AtendimentoCriadoEvento.TIPO,
                atendimentoId,
                "Paciente Teste",
                "paciente@example.com",
                LocalDateTime.of(2026, 10, 10, 14, 30)
        );
    }
}
