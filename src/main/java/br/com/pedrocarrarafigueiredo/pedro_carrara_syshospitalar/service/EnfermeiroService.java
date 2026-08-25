package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnfermeiroService extends BaseService<Enfermeiro> {

    public List<Enfermeiro> filtrarPorSetor(String setor) {
        if (setor == null || setor.isBlank()) {
            throw new IllegalArgumentException("Setor e obrigatorio.");
        }

        return buscarTodos()
                .stream()
                .filter(enfermeiro -> enfermeiro.trabalhaNoSetor(setor))
                .toList();
    }

    public List<Enfermeiro> listarAtivos() {
        return buscarTodos()
                .stream()
                .filter(Enfermeiro::isAtivo)
                .toList();
    }
}
