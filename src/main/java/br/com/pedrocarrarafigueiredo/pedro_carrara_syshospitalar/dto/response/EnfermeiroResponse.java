package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.dto.response;

public record EnfermeiroResponse(
        Long id,
        String nome,
        int idade,
        String cpf,
        String email,
        boolean ativo,
        String coren,
        String setor
) {
}
