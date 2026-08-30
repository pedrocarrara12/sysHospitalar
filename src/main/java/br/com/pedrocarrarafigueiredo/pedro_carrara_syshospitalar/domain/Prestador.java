package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private int idade;
    private String cpf;
    private String email;
    private boolean ativo;

    public Prestador() {
        this.ativo = true;
    }

    public Prestador(String nome, int idade, String cpf, String email, Boolean ativo) {
        this(null, nome, idade, cpf, email, ativo);
    }

    public Prestador(Long id, String nome, int idade, String cpf, String email, Boolean ativo) {
        if (id != null) {
            atribuirId(id);
        }
        setNome(nome);
        setIdade(idade);
        setCpf(cpf);
        setEmail(email);
        this.ativo = ativo == null ? true : ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        if (id != null) {
            validarId(id);
        }
        this.id = id;
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id do prestador deve ser positivo.");
        }
    }

    protected void atribuirId(Long id) {
        setId(id);
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
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", idade=" + idade +
                ", cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", ativo=" + ativo +
                '}';
    }
}
