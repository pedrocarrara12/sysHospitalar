package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto;

public record PacienteCsvItem(
        int numeroLinha,
        String nome,
        String cpf,
        String dataNascimento,
        String sexo,
        String telefone,
        String email,
        String ativo
) {
}
