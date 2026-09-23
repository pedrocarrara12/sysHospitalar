package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.client;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoServiceResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "atendimentos-service",
        url = "${services.atendimento.url}"
)
public interface AtendimentoClient {

    @GetMapping("/atendimentos")
    List<AtendimentoServiceResponse> buscarTodos();

    @GetMapping("/atendimentos/{id}")
    AtendimentoServiceResponse buscarPorId(@PathVariable("id") Long id);

    @GetMapping("/atendimentos/filtro/status")
    List<AtendimentoServiceResponse> filtrarPorStatus(@RequestParam("status") StatusAtendimento status);

    @GetMapping("/atendimentos/filtro/tipo")
    List<AtendimentoServiceResponse> filtrarPorTipo(@RequestParam("tipo") TipoAtendimento tipo);

    @GetMapping("/atendimentos/ordenados-por-data")
    List<AtendimentoServiceResponse> listarOrdenadoPorDataHora();

    @PostMapping("/atendimentos")
    AtendimentoServiceResponse cadastrar(@RequestBody AtendimentoRequest request);

    @PutMapping("/atendimentos/{id}")
    AtendimentoServiceResponse atualizar(@PathVariable("id") Long id, @RequestBody AtendimentoRequest request);

    @DeleteMapping("/atendimentos/{id}")
    void remover(@PathVariable("id") Long id);

}
