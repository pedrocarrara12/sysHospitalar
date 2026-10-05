package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enfermeiro.repository.EnfermeiroRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.medico.repository.MedicoRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.dto.request.PacienteRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.service.PacienteService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ContemObjetoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.PacienteImportacaoService;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteInicioResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteStatusResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PedroCarraraSyshospitalarApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private PacienteImportacaoService pacienteImportacaoService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private EnfermeiroRepository enfermeiroRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void prepararMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

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

    @Test
    @Transactional
    void normalizaEmailEImpedeCpfOuEmailDuplicado() {
        PacienteRequest primeiro = new PacienteRequest(
                "Maria Unica", "98765432100", LocalDate.of(1990, 5, 10),
                "F", "65999990000", "  MARIA.UNICA@EMAIL.COM  ", true);

        Paciente salvo = pacienteService.cadastrar(primeiro);

        assertThat(salvo.getEmail()).isEqualTo("maria.unica@email.com");

        PacienteRequest cpfDuplicado = new PacienteRequest(
                "Outra Pessoa", "98765432100", LocalDate.of(1991, 6, 11),
                "F", "65999990001", "outra@email.com", true);
        assertThatThrownBy(() -> pacienteService.cadastrar(cpfDuplicado))
                .isInstanceOf(ContemObjetoException.class)
                .hasMessageContaining("CPF");

        PacienteRequest emailDuplicado = new PacienteRequest(
                "Terceira Pessoa", "98765432101", LocalDate.of(1992, 7, 12),
                "M", "65999990002", "MARIA.UNICA@EMAIL.COM", true);
        assertThatThrownBy(() -> pacienteService.cadastrar(emailDuplicado))
                .isInstanceOf(ContemObjetoException.class)
                .hasMessageContaining("E-mail");
    }

    @Test
    void importaPacientesEmChunksNormalizandoEFiltrandoLinhas() throws Exception {
        String csv = "nome,cpf,dataNascimento,sexo,telefone,email,ativo\n"
                + "  Carlos   Batch  ,321.654.987-00,1985-03-20,m,(65) 99999-0000,CARLOS.BATCH@EMAIL.COM,\n"
                + "Data Futura,11122233344,2999-01-01,F,65999990001,futura@email.com,true\n"
                + "Outro Carlos,99988877766,1980-01-01,M,65999990002,carlos.batch@email.com,false\n";
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "pacientes.csv", "text/csv", csv.getBytes());

        ImportacaoPacienteInicioResponse inicio = pacienteImportacaoService.iniciar(arquivo);
        Path temporario = caminhoTemporario(inicio.executionId());
        ImportacaoPacienteStatusResponse status = aguardarConclusao(inicio.executionId());

        assertThat(status.status()).isEqualTo("COMPLETED");
        assertThat(status.lidos()).isEqualTo(3);
        assertThat(status.gravados()).isEqualTo(1);
        assertThat(status.filtrados()).isEqualTo(2);
        Paciente importado = pacienteRepository.findAll().stream()
                .filter(paciente -> paciente.getCpf().equals("32165498700"))
                .findFirst()
                .orElseThrow();
        assertThat(importado.getNome()).isEqualTo("Carlos Batch");
        assertThat(importado.getTelefone()).isEqualTo("65999990000");
        assertThat(importado.getEmail()).isEqualTo("carlos.batch@email.com");
        assertThat(importado.isAtivo()).isTrue();
        assertThat(temporario).doesNotExist();
    }

    @Test
    void rejeitaCsvComCabecalhoInvalidoAntesDeCriarJob() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "pacientes.csv", "text/csv", "nome,email\nAna,ana@email.com".getBytes());

        assertThatThrownBy(() -> pacienteImportacaoService.iniciar(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabecalho CSV invalido");
    }

    @Test
    void iniciaEConsultaImportacaoPelosEndpointsPublicos() throws Exception {
        String csv = "nome,cpf,dataNascimento,sexo,telefone,email,ativo\n"
                + "Paciente Endpoint,55544433322,1990-01-15,F,65999990003,endpoint@email.com,true\n";
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "pacientes-endpoint.csv", "text/csv", csv.getBytes());

        MvcResult resultado = mockMvc.perform(multipart("/batch/pacientes/importacoes").file(arquivo))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "/batch/pacientes/importacoes/\\d+")))
                .andExpect(jsonPath("$.jobName").value("importacaoPacientesJob"))
                .andExpect(jsonPath("$.arquivo").value("pacientes-endpoint.csv"))
                .andReturn();

        String location = resultado.getResponse().getHeader("Location");
        assertThat(location).isNotBlank();
        long executionId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        aguardarConclusao(executionId);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.gravados").value(1));
    }

    @Test
    void retorna404AoConsultarExecucaoInexistente() throws Exception {
        mockMvc.perform(get("/batch/pacientes/importacoes/{executionId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void preservaArquivoQuandoLimiteDeErrosEstruturaisFazJobFalhar() throws Exception {
        StringBuilder csv = new StringBuilder("nome,cpf,dataNascimento,sexo,telefone,email,ativo\n");
        for (int linha = 0; linha < 101; linha++) {
            csv.append("linha-malformada\n");
        }
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "pacientes-malformados.csv", "text/csv", csv.toString().getBytes());

        ImportacaoPacienteInicioResponse inicio = pacienteImportacaoService.iniciar(arquivo);
        Path temporario = caminhoTemporario(inicio.executionId());
        try {
            ImportacaoPacienteStatusResponse status = aguardarConclusao(inicio.executionId());
            assertThat(status.status()).isEqualTo("FAILED");
            assertThat(temporario).exists();
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private ImportacaoPacienteStatusResponse aguardarConclusao(long executionId) throws InterruptedException {
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            ImportacaoPacienteStatusResponse status = pacienteImportacaoService.buscarStatus(executionId);
            if (status.status().equals("COMPLETED") || status.status().equals("FAILED")) {
                return status;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("A importacao nao terminou dentro do tempo esperado");
    }

    private Path caminhoTemporario(long executionId) {
        var execucao = jobRepository.getJobExecution(executionId);
        assertThat(execucao).isNotNull();
        return Path.of(execucao.getJobParameters().getString("arquivoPath"));
    }
}
