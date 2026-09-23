package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.domain;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Objects;
@Entity
@Table(name = "atendimento")
public class Atendimento implements Comparable<Atendimento> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataHoraAtendimento;

    @Enumerated(EnumType.STRING)
    private TipoAtendimento tipoAtendimento;

    @Enumerated(EnumType.STRING)
    private StatusAtendimento statusAtendimento;

    private Long pacienteId;

    private Long medicoId;

    public Atendimento() {
    }

    public Atendimento(LocalDateTime dataHoraAtendimento,
        TipoAtendimento tipoAtendimento, StatusAtendimento statusAtendimento,
        Long pacienteId, Long medicoId) {
        setDataHoraAtendimento(dataHoraAtendimento);
        setTipoAtendimento(tipoAtendimento);
        setStatusAtendimento(statusAtendimento);
        setPacienteId(pacienteId);
        setMedicoId(medicoId);
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
            throw new IllegalArgumentException("Id do atendimento deve ser positivo.");
        }
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

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        if (pacienteId == null || pacienteId <= 0) {
            throw new IllegalArgumentException("Id do paciente deve ser positivo.");
        }

        this.pacienteId = pacienteId;
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        if (medicoId == null || medicoId <= 0) {
            throw new IllegalArgumentException("Id do medico deve ser positivo.");
        }

        this.medicoId = medicoId;
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
                ", pacienteId=" + pacienteId +
                ", medicoId=" + medicoId +
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
