package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ContemObjetoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteInicioResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteStatusResponse;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PacienteImportacaoService {
    public static final String JOB_NAME = "importacaoPacientesJob";
    private static final String CABECALHO = "nome,cpf,dataNascimento,sexo,telefone,email,ativo";
    private static final long TAMANHO_MAXIMO = 2L * 1024 * 1024;

    private final JobOperator jobOperator;
    private final Job importacaoPacientesJob;
    private final JobRepository jobRepository;
    private final Path diretorio;

    public PacienteImportacaoService(
            JobOperator jobOperator,
            Job importacaoPacientesJob,
            JobRepository jobRepository,
            @Value("${batch.pacientes.diretorio:${java.io.tmpdir}/syshospitalar/batch}") String diretorio) {
        this.jobOperator = jobOperator;
        this.importacaoPacientesJob = importacaoPacientesJob;
        this.jobRepository = jobRepository;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public synchronized ImportacaoPacienteInicioResponse iniciar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        if (!jobRepository.findRunningJobExecutions(JOB_NAME).isEmpty()) {
            throw new ContemObjetoException("Ja existe uma importacao de pacientes em andamento");
        }

        String nomeOriginal = nomeSeguro(arquivo.getOriginalFilename());
        Path temporario = null;
        try {
            byte[] conteudo = arquivo.getBytes();
            validarCabecalho(conteudo);
            Files.createDirectories(diretorio);
            temporario = diretorio.resolve(UUID.randomUUID() + ".csv").normalize();
            if (!temporario.startsWith(diretorio)) {
                throw new IllegalArgumentException("Caminho de arquivo invalido");
            }
            Files.write(temporario, conteudo, StandardOpenOption.CREATE_NEW);

            JobParameters parametros = new JobParametersBuilder()
                    .addString("execucaoUuid", UUID.randomUUID().toString())
                    .addString("arquivoPath", temporario.toString(), false)
                    .addString("nomeOriginal", nomeOriginal, false)
                    .toJobParameters();
            JobExecution execucao = jobOperator.start(importacaoPacientesJob, parametros);
            return new ImportacaoPacienteInicioResponse(
                    execucao.getId(),
                    JOB_NAME,
                    execucao.getStatus().name(),
                    nomeOriginal,
                    execucao.getCreateTime()
            );
        } catch (IllegalArgumentException exception) {
            apagarSilenciosamente(temporario);
            throw exception;
        } catch (Exception exception) {
            apagarSilenciosamente(temporario);
            throw new IllegalArgumentException("Nao foi possivel iniciar a importacao de pacientes", exception);
        }
    }

    public ImportacaoPacienteStatusResponse buscarStatus(long executionId) {
        JobExecution execucao = jobRepository.getJobExecution(executionId);
        if (execucao == null || !JOB_NAME.equals(execucao.getJobInstance().getJobName())) {
            throw new ObjetoNaoEncontradoException("Execucao de importacao nao encontrada");
        }

        long lidos = 0;
        long gravados = 0;
        long filtrados = 0;
        long ignorados = 0;
        for (StepExecution step : execucao.getStepExecutions()) {
            lidos += step.getReadCount();
            gravados += step.getWriteCount();
            filtrados += step.getFilterCount();
            ignorados += step.getSkipCount();
        }

        return new ImportacaoPacienteStatusResponse(
                execucao.getId(),
                execucao.getStatus().name(),
                execucao.getStartTime() == null ? execucao.getCreateTime() : execucao.getStartTime(),
                execucao.getEndTime(),
                lidos,
                gravados,
                filtrados,
                ignorados,
                resumo(execucao)
        );
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV e obrigatorio");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("Arquivo CSV deve ter no maximo 2 MB");
        }
        String nome = nomeSeguro(arquivo.getOriginalFilename());
        if (!nome.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Arquivo deve possuir extensao .csv");
        }
    }

    private void validarCabecalho(byte[] conteudo) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8))) {
            String cabecalho = reader.readLine();
            if (cabecalho == null || !CABECALHO.equals(cabecalho.replace("\uFEFF", "").trim())) {
                throw new IllegalArgumentException("Cabecalho CSV invalido. Esperado: " + CABECALHO);
            }
        }
    }

    private String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo CSV e obrigatorio");
        }
        return Path.of(nomeOriginal).getFileName().toString();
    }

    private String resumo(JobExecution execucao) {
        return switch (execucao.getStatus()) {
            case COMPLETED -> "Importacao concluida";
            case FAILED -> "Importacao falhou: verifique os logs da execucao";
            case STARTED, STARTING -> "Importacao em andamento";
            default -> "Importacao com status " + execucao.getStatus().name();
        };
    }

    private void apagarSilenciosamente(Path arquivo) {
        if (arquivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ignored) {
            // O erro original da inicializacao e mais relevante para o cliente.
        }
    }
}
