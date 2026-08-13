package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

public class Enfermeiro extends Prestador {

    private String coren;
    private String setor;

    public Enfermeiro() {
    }

    public Enfermeiro(String coren, String setor) {
        setCoren(coren);
        setSetor(setor);
    }

    public Enfermeiro(String nome, int idade, String cpf, String email, Boolean ativo, String coren, String setor) {
        super(nome, idade, cpf, email, ativo);
        setCoren(coren);
        setSetor(setor);
    }

    public String getCoren() {
        return coren;
    }

    public void setCoren(String coren) {
        if (coren == null || coren.isBlank()) {
            throw new IllegalArgumentException("COREN do enfermeiro e obrigatorio.");
        }

        this.coren = coren;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        if (setor == null || setor.isBlank()) {
            throw new IllegalArgumentException("Setor do enfermeiro e obrigatorio.");
        }

        this.setor = setor;
    }

    public boolean trabalhaNoSetor(String setorDesejado) {
        return setorDesejado != null && setor.equalsIgnoreCase(setorDesejado);
    }

    @Override
    public String obterRegistroProfissional() {
        return coren;
    }

    @Override
    public String toString() {
        return "Enfermeiro{" +
                "nome='" + getNome() + '\'' +
                ", idade=" + getIdade() +
                ", cpf='" + getCpf() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", ativo=" + isAtivo() +
                ", coren='" + coren + '\'' +
                ", setor='" + setor + '\'' +
                '}';
    }
}
