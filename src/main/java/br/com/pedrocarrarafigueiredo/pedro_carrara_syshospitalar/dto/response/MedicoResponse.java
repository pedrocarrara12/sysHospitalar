package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response;

public record MedicoResponse(
        Long id,
        String nome,
        int idade,
        String cpf,
        String email,
        boolean ativo,
        String crm,
        String especialidade
) {
}
