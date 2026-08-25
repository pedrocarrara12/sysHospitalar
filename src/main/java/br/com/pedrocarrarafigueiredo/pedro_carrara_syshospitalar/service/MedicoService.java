package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService extends BaseService<Medico> {

    public List<Medico> filtrarPorEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            throw new IllegalArgumentException("Especialidade e obrigatoria.");
        }

        return buscarTodos()
                .stream()
                .filter(medico -> medico.atendeEspecialidade(especialidade))
                .toList();
    }

    public List<Medico> listarAtivos() {
        return buscarTodos()
                .stream()
                .filter(Medico::isAtivo)
                .toList();
    }
}
