package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnfermeiroRequest(
        @NotBlank(message = "Nome do enfermeiro e obrigatorio.")
        String nome,

        @NotNull(message = "Idade do enfermeiro e obrigatoria.")
        @Min(value = 18, message = "Enfermeiro deve ter pelo menos 18 anos.")
        Integer idade,

        @NotBlank(message = "CPF e obrigatorio.")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 numeros.")
        String cpf,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        Boolean ativo,

        @NotBlank(message = "COREN do enfermeiro e obrigatorio.")
        String coren,

        @NotBlank(message = "Setor do enfermeiro e obrigatorio.")
        String setor
) {
}
