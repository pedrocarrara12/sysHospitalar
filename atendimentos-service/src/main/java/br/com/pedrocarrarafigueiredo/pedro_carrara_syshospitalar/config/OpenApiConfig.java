package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sistema Hospitalar API",
                version = "1.0",
                description = "API REST para gerenciamento de pacientes, médicos, enfermeiros e atendimentos"
        )
)
public class OpenApiConfig {
}
