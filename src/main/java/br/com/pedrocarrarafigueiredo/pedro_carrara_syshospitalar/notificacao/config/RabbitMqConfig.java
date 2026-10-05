package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange atendimentosExchange(
            @Value("${notificacoes.rabbitmq.exchange:atendimentos.exchange}") String nome) {
        return new DirectExchange(nome, true, false);
    }

    @Bean
    DirectExchange atendimentosDeadLetterExchange(
            @Value("${notificacoes.rabbitmq.dlx:atendimentos.dlx}") String nome) {
        return new DirectExchange(nome, true, false);
    }

    @Bean
    Queue atendimentoCriadoQueue(
            @Value("${notificacoes.rabbitmq.queue:notificacao-email.atendimento-criado}") String nome,
            @Value("${notificacoes.rabbitmq.dlx:atendimentos.dlx}") String dlx,
            @Value("${notificacoes.rabbitmq.dlq-routing-key:atendimento.criado.falhou}") String dlqRoutingKey) {
        return QueueBuilder.durable(nome)
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }

    @Bean
    Queue atendimentoCriadoDeadLetterQueue(
            @Value("${notificacoes.rabbitmq.dlq:notificacao-email.atendimento-criado.dlq}") String nome) {
        return QueueBuilder.durable(nome).build();
    }

    @Bean
    Binding atendimentoCriadoBinding(
            Queue atendimentoCriadoQueue,
            DirectExchange atendimentosExchange,
            @Value("${notificacoes.rabbitmq.routing-key:atendimento.criado}") String routingKey) {
        return BindingBuilder.bind(atendimentoCriadoQueue).to(atendimentosExchange).with(routingKey);
    }

    @Bean
    Binding atendimentoCriadoDeadLetterBinding(
            Queue atendimentoCriadoDeadLetterQueue,
            DirectExchange atendimentosDeadLetterExchange,
            @Value("${notificacoes.rabbitmq.dlq-routing-key:atendimento.criado.falhou}") String routingKey) {
        return BindingBuilder.bind(atendimentoCriadoDeadLetterQueue)
                .to(atendimentosDeadLetterExchange)
                .with(routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    Advice notificacaoRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(2000, 2.0, 4000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            Advice notificacaoRetryInterceptor,
            @Value("${notificacoes.consumidor.ativo:true}") boolean consumidorAtivo) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAdviceChain(new Advice[]{notificacaoRetryInterceptor});
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(consumidorAtivo);
        return factory;
    }
}
