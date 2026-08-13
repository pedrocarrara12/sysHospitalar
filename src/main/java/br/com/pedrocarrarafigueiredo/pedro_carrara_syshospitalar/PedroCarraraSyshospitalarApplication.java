package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PedroCarraraSyshospitalarApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(PedroCarraraSyshospitalarApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Medico medico = new Medico(
                "Ana Martins",
                42,
                "12345678901",
                "ana.martins@hospital.com",
                true,
                "CRM-MT-12345",
                "Cardiologia"
        );

        Enfermeiro enfermeiro = new Enfermeiro(
                "Carlos Lima",
                35,
                "98765432100",
                "carlos.lima@hospital.com",
                true,
                "COREN-MT-99887",
                "Pronto Atendimento"
        );

        Paciente paciente = new Paciente(
                "Joao Pereira",
                "11122233344",
                LocalDate.of(1988, 3, 22),
                'M',
                "65999990000",
                "joao.pereira@email.com",
                true
        );

        Atendimento primeiroAtendimento = new Atendimento(
                1L,
                LocalDateTime.of(2026, 8, 13, 9, 30),
                TipoAtendimento.AMBULATORIAL,
                StatusAtendimento.ANDAMENTO,
                paciente,
                medico
        );

        Atendimento segundoAtendimento = new Atendimento(
                2L,
                LocalDateTime.of(2026, 8, 13, 10, 45),
                TipoAtendimento.URGENCIA,
                StatusAtendimento.ANDAMENTO,
                paciente,
                medico
        );

        paciente.adicionarAtendimento(primeiroAtendimento);
        paciente.adicionarAtendimento(segundoAtendimento);
        segundoAtendimento.concluir();

        System.out.println("=== Etapa 1: Modelo orientado a objetos ===");
        System.out.println(medico);
        System.out.println(enfermeiro);
        System.out.println(paciente);
        System.out.println("Quantidade de atendimentos do paciente: " + paciente.quantidadeAtendimentos());
        System.out.println("Paciente possui atendimento em andamento? " + paciente.possuiAtendimentoEmAndamento());
        System.out.println("Medico atende cardiologia? " + medico.atendeEspecialidade("Cardiologia"));
        System.out.println("Enfermeiro trabalha no pronto atendimento? " + enfermeiro.trabalhaNoSetor("Pronto Atendimento"));
        System.out.println("Atendimentos:");

        paciente.getAtendimentos().forEach(atendimento -> System.out.println(atendimento));
    }
}
