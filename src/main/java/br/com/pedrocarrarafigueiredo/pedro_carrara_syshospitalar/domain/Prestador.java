package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

public abstract class Prestador {

    private String nome;
    private int idade;
    private String cpf;
    private String email;
    private boolean ativo;

    public Prestador() {
        this.ativo = true;
    }

    public Prestador(String nome, int idade, String cpf, String email, Boolean ativo) {
        setNome(nome);
        setIdade(idade);
        setCpf(cpf);
        setEmail(email);
        this.ativo = ativo == null ? true : ativo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do prestador e obrigatorio.");
        }

        this.nome = nome;
    }

    public int getIdade() {
        return idade;
    }

    public void setIdade(int idade) {
        if (idade < 18) {
            throw new IllegalArgumentException("Prestador deve ter pelo menos 18 anos.");
        }

        this.idade = idade;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF deve conter exatamente 11 numeros.");
        }

        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("E-mail invalido.");
        }

        this.email = email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void inativar() {
        this.ativo = false;
    }

    public boolean podeRealizarAtendimento() {
        return ativo;
    }

    public abstract String obterRegistroProfissional();

    @Override
    public String toString() {
        return "Prestador{" +
                "nome='" + nome + '\'' +
                ", idade=" + idade +
                ", cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", ativo=" + ativo +
                '}';
    }
}
