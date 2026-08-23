package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;

import java.util.List;

public class EnfermeiroService extends BaseService<Enfermeiro> {

    public List<Enfermeiro> filtrarPorSetor(String setor) {
        if (setor == null || setor.isBlank()) {
            throw new IllegalArgumentException("Setor e obrigatorio.");
        }

        return buscarTodos()
                .values()
                .stream()
                .filter(enfermeiro -> enfermeiro.trabalhaNoSetor(setor))
                .toList();
    }

    public List<Enfermeiro> listarAtivos() {
        return buscarTodos()
                .values()
                .stream()
                .filter(Enfermeiro::isAtivo)
                .toList();
    }
}
