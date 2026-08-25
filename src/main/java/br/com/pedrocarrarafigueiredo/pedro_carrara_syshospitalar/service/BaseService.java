package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Identificavel;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ContemObjetoException;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.exception.ObjetoNaoEncontradoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseService<T extends Identificavel> {

    private final Map<Long, T> map = new HashMap<>();
    private Long proximoId = 1L;

    public T cadastrar(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Objeto nao pode ser nulo");
        }

        if (map.containsValue(obj)) {
            throw new ContemObjetoException("Objeto ja cadastrado");
        }

        Long id = proximoId++;
        obj.setId(id);
        map.put(id, obj);
        return obj;
    }

    public T atualizar(Long id, T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Objeto nao pode ser nulo");
        }

        buscarPorId(id);
        obj.setId(id);
        map.put(id, obj);
        return obj;
    }

    public void remover(Long id) {
        buscarPorId(id);
        map.remove(id);
    }
    
    public T buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id nao pode ser nulo");
        }

        if (!map.containsKey(id)) {
            throw new ObjetoNaoEncontradoException("Objeto nao encontrado");
        }

        return map.get(id);
    }

    public List<T> buscarTodos() {
        return new ArrayList<>(map.values());
    }
}
