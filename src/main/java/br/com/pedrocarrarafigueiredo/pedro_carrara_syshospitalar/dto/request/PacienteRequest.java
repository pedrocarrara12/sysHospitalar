package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank(message = "Nome do paciente e obrigatorio.")
        String nome,

        @NotBlank(message = "CPF e obrigatorio.")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 numeros.")
        String cpf,

        @NotNull(message = "Data de nascimento e obrigatoria.")
        @PastOrPresent(message = "Data de nascimento nao pode ser futura.")
        LocalDate dataNascimento,

        @NotBlank(message = "Sexo e obrigatorio.")
        @Pattern(regexp = "[MmFf]", message = "Sexo deve ser M ou F.")
        String sexo,

        @NotBlank(message = "Telefone do paciente e obrigatorio.")
        String telefone,

        @NotBlank(message = "E-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        Boolean ativo
) {
}
