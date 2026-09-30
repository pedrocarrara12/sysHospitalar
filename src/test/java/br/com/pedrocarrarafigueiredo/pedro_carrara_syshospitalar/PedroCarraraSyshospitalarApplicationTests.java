package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.repository.EnfermeiroRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PedroCarraraSyshospitalarApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private EnfermeiroRepository enfermeiroRepository;

    @Test
    @Transactional
    void persisteEntidadesPrincipaisNoPostgreSql() {
        Paciente paciente = pacienteRepository.saveAndFlush(new Paciente(
                "Maria Teste", "12345678901", LocalDate.of(1990, 5, 10),
                'F', "65999990000", "maria.teste@email.com", true));

        Medico medico = medicoRepository.saveAndFlush(new Medico(
                "Joao Teste", 40, "10987654321", "joao.teste@email.com",
                true, "CRM-TESTE", "Cardiologia"));

        Enfermeiro enfermeiro = enfermeiroRepository.saveAndFlush(new Enfermeiro(
                "Ana Teste", 32, "11122233344", "ana.teste@email.com",
                true, "COREN-TESTE", "UTI"));

        assertThat(pacienteRepository.findById(paciente.getId())).isPresent();
        assertThat(medicoRepository.findById(medico.getId())).isPresent();
        assertThat(enfermeiroRepository.findById(enfermeiro.getId())).isPresent();
    }
}
