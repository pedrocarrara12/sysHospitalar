package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.notificacao.evento.AtendimentoCriadoEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailAtendimentoService {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailAtendimentoService(
            JavaMailSender mailSender,
            @Value("${notificacoes.email.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviarConfirmacao(AtendimentoCriadoEvento evento) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.pacienteEmail());
        mensagem.setSubject("Confirmação de atendimento #" + evento.atendimentoId());
        mensagem.setText("Olá, " + evento.pacienteNome() + ".\n"
                + "Seu atendimento #" + evento.atendimentoId() + " foi registrado para "
                + evento.dataHoraAtendimento().format(DATA_HORA) + ".\n"
                + "Esta é uma mensagem automática.");
        mailSender.send(mensagem);
    }
}
