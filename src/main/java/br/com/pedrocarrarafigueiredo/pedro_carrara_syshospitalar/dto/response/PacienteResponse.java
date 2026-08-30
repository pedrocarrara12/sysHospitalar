package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response;

import java.time.LocalDate;

public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        char sexo,
        String telefone,
        String email,
        boolean ativo
) {
}
