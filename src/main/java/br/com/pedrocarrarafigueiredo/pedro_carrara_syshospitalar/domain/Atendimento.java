package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;
import java.time.LocalDateTime;
import java.util.Objects;

public class Atendimento implements Identificavel, Comparable<Atendimento> {

    private Long id;
    private LocalDateTime dataHoraAtendimento;
    private TipoAtendimento tipoAtendimento;
    private StatusAtendimento statusAtendimento;
    private Paciente paciente;
    private Medico medico;

    public Atendimento(Long codigoAtendimento, LocalDateTime dataHoraAtendimento,
        TipoAtendimento tipoAtendimento, StatusAtendimento statusAtendimento,
        Paciente paciente, Medico medico) {
        setId(codigoAtendimento);
        setDataHoraAtendimento(dataHoraAtendimento);
        setTipoAtendimento(tipoAtendimento);
        setStatusAtendimento(statusAtendimento);
        setPaciente(paciente);
        setMedico(medico);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id do atendimento deve ser positivo.");
        }

        this.id = id;
    }

    public Long getCodigoAtendimento() {
        return id;
    }

    public void setCodigoAtendimento(Long codigoAtendimento) {
        setId(codigoAtendimento);
    }

    public LocalDateTime getDataHoraAtendimento() {
        return dataHoraAtendimento;
    }

    public void setDataHoraAtendimento(LocalDateTime dataHoraAtendimento) {
        if (dataHoraAtendimento == null) {
            throw new IllegalArgumentException("Data e hora do atendimento sao obrigatorias.");
        }

        this.dataHoraAtendimento = dataHoraAtendimento;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) {
        if (tipoAtendimento == null) {
            throw new IllegalArgumentException("Tipo de atendimento e obrigatorio.");
        }

        this.tipoAtendimento = tipoAtendimento;
    }

    public StatusAtendimento getStatusAtendimento() {
        return statusAtendimento;
    }

    public void setStatusAtendimento(StatusAtendimento statusAtendimento) {
        if (statusAtendimento == null) {
            throw new IllegalArgumentException("Status do atendimento e obrigatorio.");
        }

        this.statusAtendimento = statusAtendimento;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente e obrigatorio.");
        }

        this.paciente = paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        if (medico == null) {
            throw new IllegalArgumentException("Medico e obrigatorio.");
        }

        this.medico = medico;
    }

    public void iniciar() {
        if (statusAtendimento == StatusAtendimento.CANCELADO || statusAtendimento == StatusAtendimento.CONCLUIDO) {
            throw new IllegalStateException("Atendimento finalizado nao pode ser iniciado.");
        }

        statusAtendimento = StatusAtendimento.ANDAMENTO;
    }

    public void cancelar() {
        if (statusAtendimento == StatusAtendimento.CONCLUIDO) {
            throw new IllegalStateException("Atendimento concluido nao pode ser cancelado.");
        }

        statusAtendimento = StatusAtendimento.CANCELADO;
    }

    public void concluir() {
        if (statusAtendimento == StatusAtendimento.CANCELADO) {
            throw new IllegalStateException("Atendimento cancelado nao pode ser concluido.");
        }

        statusAtendimento = StatusAtendimento.CONCLUIDO;
    }

    public boolean estaEmAndamento() {
        return statusAtendimento == StatusAtendimento.ANDAMENTO;
    }

    public boolean estaConcluido() {
        return statusAtendimento == StatusAtendimento.CONCLUIDO;
    }

    @Override
    public String toString() {
        return "Atendimento{" +
                "id=" + id +
                ", dataHoraAtendimento=" + dataHoraAtendimento +
                ", tipoAtendimento=" + tipoAtendimento +
                ", statusAtendimento=" + statusAtendimento +
                ", paciente=" + paciente.getNome() +
                ", medico=" + medico.getNome() +
                '}';
    }

    @Override
    public int compareTo(Atendimento outroAtendimento) {
        return this.id.compareTo(outroAtendimento.id);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Atendimento atendimento)) {
            return false;
        }
        return Objects.equals(id, atendimento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
