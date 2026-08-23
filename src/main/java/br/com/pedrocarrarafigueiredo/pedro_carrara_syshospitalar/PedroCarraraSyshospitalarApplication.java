package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.AtendimentoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.EnfermeiroService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.MedicoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.PacienteService;
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
        MedicoService medicoService = new MedicoService();
        EnfermeiroService enfermeiroService = new EnfermeiroService();
        PacienteService pacienteService = new PacienteService();
        AtendimentoService atendimentoService = new AtendimentoService();

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

        medicoService.cadastrar(medico);
        enfermeiroService.cadastrar(enfermeiro);
        pacienteService.cadastrar(paciente);
        atendimentoService.cadastrar(primeiroAtendimento);
        atendimentoService.cadastrar(segundoAtendimento);

        Paciente pacienteAtualizado = new Paciente(
                "Joao Pereira",
                "11122233344",
                LocalDate.of(1988, 3, 22),
                'M',
                "65999991111",
                "joao.pereira@email.com",
                true
        );
        pacienteService.atualizar(paciente.getId(), pacienteAtualizado);
        enfermeiroService.remover(enfermeiro.getId());

        System.out.println("=== Etapa 2: Estruturas de Dados e Camada de Servico ===");
        System.out.println("Medico cadastrado: " + medicoService.buscarPorId(medico.getId()));
        System.out.println("Paciente atualizado: " + pacienteService.buscarPorId(pacienteAtualizado.getId()));
        System.out.println("Total de medicos cadastrados: " + medicoService.buscarTodos().size());
        System.out.println("Total de enfermeiros cadastrados apos remocao: " + enfermeiroService.buscarTodos().size());
        System.out.println("Pacientes do sexo masculino: " + pacienteService.filtrarPorSexo('M').size());
        System.out.println("Medicos ativos: " + medicoService.listarAtivos().size());
        System.out.println("Medicos de cardiologia: " + medicoService.filtrarPorEspecialidade("Cardiologia").size());
        System.out.println("Atendimentos em andamento: " + atendimentoService.filtrarPorStatus(StatusAtendimento.ANDAMENTO).size());
        System.out.println("Atendimentos ambulatoriais: " + atendimentoService.filtrarPorTipo(TipoAtendimento.AMBULATORIAL).size());
        System.out.println("Quantidade de atendimentos do paciente: " + paciente.quantidadeAtendimentos());
        System.out.println("Paciente possui atendimento em andamento? " + paciente.possuiAtendimentoEmAndamento());
        System.out.println("Medico atende cardiologia? " + medico.atendeEspecialidade("Cardiologia"));
        System.out.println("Atendimentos cadastrados:");

        atendimentoService.listarOrdenadoPorDataHora().forEach(atendimento -> System.out.println(atendimento));
    }
}
