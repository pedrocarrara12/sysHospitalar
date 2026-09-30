package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.repository.AtendimentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AtendimentosServiceApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @Test
    @Transactional
    void persisteAtendimentoNoPostgreSql() {
        Atendimento atendimento = atendimentoRepository.saveAndFlush(new Atendimento(
                LocalDateTime.of(2026, 8, 30, 20, 30),
                TipoAtendimento.AMBULATORIAL,
                StatusAtendimento.ANDAMENTO,
                1L,
                1L));

        assertThat(atendimentoRepository.findById(atendimento.getId())).isPresent();
    }
}
