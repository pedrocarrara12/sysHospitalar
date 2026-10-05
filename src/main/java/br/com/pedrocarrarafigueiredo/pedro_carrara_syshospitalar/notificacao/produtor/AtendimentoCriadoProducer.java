package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.produtor;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoCriadoProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtendimentoCriadoProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public AtendimentoCriadoProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${notificacoes.rabbitmq.exchange:atendimentos.exchange}") String exchange,
            @Value("${notificacoes.rabbitmq.routing-key:atendimento.criado}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(AtendimentoCriadoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            LOGGER.info("Evento {} publicado para o atendimento {}", evento.eventoId(), evento.atendimentoId());
        } catch (AmqpException exception) {
            LOGGER.error("Nao foi possivel publicar o evento {} do atendimento {}",
                    evento.eventoId(), evento.atendimentoId(), exception);
        }
    }
}
