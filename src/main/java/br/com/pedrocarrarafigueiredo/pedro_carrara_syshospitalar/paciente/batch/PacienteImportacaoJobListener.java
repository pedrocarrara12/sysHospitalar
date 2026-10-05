package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PacienteImportacaoJobListener implements JobExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacienteImportacaoJobListener.class);

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }

        String arquivoPath = jobExecution.getJobParameters().getString("arquivoPath");
        if (arquivoPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(arquivoPath));
        } catch (IOException exception) {
            LOGGER.warn("Nao foi possivel remover o arquivo temporario da execucao {}", jobExecution.getId());
        }
    }
}
