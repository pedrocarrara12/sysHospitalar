package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.consumidor;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.service.EmailAtendimentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoCriadoConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtendimentoCriadoConsumer.class);

    private final EmailAtendimentoService emailAtendimentoService;

    public AtendimentoCriadoConsumer(EmailAtendimentoService emailAtendimentoService) {
        this.emailAtendimentoService = emailAtendimentoService;
    }

    @RabbitListener(queues = "${notificacoes.rabbitmq.queue:notificacao-email.atendimento-criado}")
    public void consumir(AtendimentoCriadoEvento evento) {
        LOGGER.info("Processando evento {} do atendimento {}", evento.eventoId(), evento.atendimentoId());
        emailAtendimentoService.enviarConfirmacao(evento);
        LOGGER.info("Evento {} processado com sucesso", evento.eventoId());
    }
}
