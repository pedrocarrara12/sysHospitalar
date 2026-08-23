package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;

import java.util.List;

public class PacienteService extends BaseService<Paciente> {
    public List<Paciente> filtrarPorSexo(char sexo) {

        if (!isSexoValido(sexo)) {
            throw new IllegalArgumentException("Sexo invalido. Informe M ou F.");
        }

        char sexoNormalizado = Character.toUpperCase(sexo);

        return buscarTodos()
                .values()
                .stream()
                .filter(paciente -> Character.toUpperCase(paciente.getSexo()) == sexoNormalizado)
                .toList();
    }

    public List<Paciente> listarOrdenadoPorNome() {
        return buscarTodos()
                .values()
                .stream()
                .sorted()
                .toList();
    }

    private boolean isSexoValido(char sexo) {
        char sexoNormalizado = Character.toUpperCase(sexo);
        return sexoNormalizado == 'M' || sexoNormalizado == 'F';
    }

}
