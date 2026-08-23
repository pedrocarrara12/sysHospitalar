package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.enuns.TipoAtendimento;

import java.util.Comparator;
import java.util.List;

public class AtendimentoService extends BaseService<Atendimento> {

    public List<Atendimento> filtrarPorStatus(StatusAtendimento status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do atendimento e obrigatorio.");
        }

        return buscarTodos()
                .values()
                .stream()
                .filter(atendimento -> atendimento.getStatusAtendimento() == status)
                .toList();
    }

    public List<Atendimento> filtrarPorTipo(TipoAtendimento tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de atendimento e obrigatorio.");
        }

        return buscarTodos()
                .values()
                .stream()
                .filter(atendimento -> atendimento.getTipoAtendimento() == tipo)
                .toList();
    }

    public List<Atendimento> listarOrdenadoPorDataHora() {
        return buscarTodos()
                .values()
                .stream()
                .sorted(Comparator.comparing(Atendimento::getDataHoraAtendimento))
                .toList();
    }
}
