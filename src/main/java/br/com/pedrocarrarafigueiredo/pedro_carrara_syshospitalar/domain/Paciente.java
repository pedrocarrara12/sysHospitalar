package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class Paciente implements Identificavel {

    private Long id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private char sexo;
    private String telefone;
    private String email;
    private boolean ativo;

    private final Set<Atendimento> atendimentos = new TreeSet<>();

    public Paciente() {
        this.ativo = true;
    }

    public Paciente(String nome, String cpf,
        LocalDate dataNascimento, char sexo, String telefone, String email, Boolean ativo) {
        this(null, nome, cpf, dataNascimento, sexo, telefone, email, ativo);
    }

    public Paciente(Long id, String nome, String cpf,
        LocalDate dataNascimento, char sexo, String telefone, String email, Boolean ativo) {
        if (id != null) {
            setId(id);
        }
        setNome(nome);
        setCpf(cpf);
        setDataNascimento(dataNascimento);
        setSexo(sexo);
        setTelefone(telefone);
        setEmail(email);
        this.ativo = ativo == null ? true : ativo;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id do paciente deve ser positivo.");
        }

        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do paciente e obrigatorio.");
        }

        this.nome = nome;
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

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento invalida.");
        }

        this.dataNascimento = dataNascimento;
    }

    public char getSexo() {
        return sexo;
    }

    public void setSexo(char sexo) {
        char sexoNormalizado = Character.toUpperCase(sexo);
        if (sexoNormalizado != 'M' && sexoNormalizado != 'F') {
            throw new IllegalArgumentException("Sexo deve ser M ou F.");
        }

        this.sexo = sexoNormalizado;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("Telefone do paciente e obrigatorio.");
        }

        this.telefone = telefone;
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

    public Set<Atendimento> getAtendimentos() {
        return Collections.unmodifiableSet(atendimentos);
    }

    public void adicionarAtendimento(Atendimento atendimento) {
        if (atendimento == null) {
            throw new IllegalArgumentException("Atendimento nao pode ser nulo.");
        }

        atendimentos.add(atendimento);
    }

    public boolean removerAtendimento(Atendimento atendimento) {
        return atendimentos.remove(atendimento);
    }

    public int quantidadeAtendimentos() {
        return atendimentos.size();
    }

    public int calcularIdade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public boolean possuiAtendimentoEmAndamento() {
        return atendimentos.stream().anyMatch(Atendimento::estaEmAndamento);
    }

    @Override
    public String toString() {
        return "Paciente{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", dataNascimento=" + dataNascimento +
                ", idade=" + calcularIdade() +
                ", sexo=" + sexo +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                ", ativo=" + ativo +
                ", quantidadeAtendimentos=" + atendimentos.size() +
                '}';
    }
}
