package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

public class Medico extends Prestador {

    private String crm;
    private String especialidade;

    public Medico() {
    }

    public Medico(String nome, int idade, String cpf, String email, Boolean ativo, String crm, String especialidade) {
        super(nome, idade, cpf, email, ativo);
        setCrm(crm);
        setEspecialidade(especialidade);
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        if (crm == null || crm.isBlank()) {
            throw new IllegalArgumentException("CRM do medico e obrigatorio.");
        }

        this.crm = crm;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            throw new IllegalArgumentException("Especialidade do medico e obrigatoria.");
        }

        this.especialidade = especialidade;
    }

    public boolean atendeEspecialidade(String especialidadeDesejada) {
        return especialidadeDesejada != null && especialidade.equalsIgnoreCase(especialidadeDesejada);
    }

    @Override
    public String obterRegistroProfissional() {
        return crm;
    }

    @Override
    public String toString() {
        return "Medico{" +
                "nome='" + getNome() + '\'' +
                ", idade=" + getIdade() +
                ", cpf='" + getCpf() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", ativo=" + isAtivo() +
                ", crm='" + crm + '\'' +
                ", especialidade='" + especialidade + '\'' +
                '}';
    }
}
