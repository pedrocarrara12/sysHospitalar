package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.PacienteCsvItem;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.repository.PacienteRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PacienteBatchConfig {

    @Bean
    @BatchTaskExecutor
    AsyncTaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("importacao-pacientes-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    @StepScope
    FlatFileItemReader<PacienteCsvItem> pacienteCsvReader(
            @Value("#{jobParameters['arquivoPath']}") String arquivoPath) {
        return new FlatFileItemReaderBuilder<PacienteCsvItem>()
                .name("pacienteCsvReader")
                .resource(new FileSystemResource(arquivoPath))
                .encoding("UTF-8")
                .linesToSkip(1)
                .lineMapper(new PacienteCsvLineMapper())
                .strict(true)
                .build();
    }

    @Bean
    @StepScope
    PacienteImportacaoProcessor pacienteImportacaoProcessor(PacienteRepository pacienteRepository) {
        return new PacienteImportacaoProcessor(pacienteRepository);
    }

    @Bean
    JpaItemWriter<Paciente> pacienteJpaWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Paciente>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true)
                .build();
    }

    @Bean
    Step importarPacientesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<PacienteCsvItem> pacienteCsvReader,
            PacienteImportacaoProcessor pacienteImportacaoProcessor,
            JpaItemWriter<Paciente> pacienteJpaWriter) {
        return new StepBuilder("importarPacientesStep", jobRepository)
                .<PacienteCsvItem, Paciente>chunk(10)
                .transactionManager(transactionManager)
                .reader(pacienteCsvReader)
                .processor(pacienteImportacaoProcessor)
                .writer(pacienteJpaWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    Job importacaoPacientesJob(JobRepository jobRepository, Step importarPacientesStep) {
        return new JobBuilder("importacaoPacientesJob", jobRepository)
                .listener(new PacienteImportacaoJobListener())
                .start(importarPacientesStep)
                .build();
    }
}
