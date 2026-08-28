package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.AtendimentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atendimentos")
public class AtendimentoController {
    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping
    public ResponseEntity<List<Atendimento>> buscarTodos() {
        return ResponseEntity.ok(atendimentoService.buscarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Atendimento> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Atendimento> inserir(@RequestBody Atendimento atendimento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atendimentoService.cadastrar(atendimento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Atendimento> atualizar(@PathVariable Long id, @RequestBody Atendimento atendimento) {
        return ResponseEntity.ok(atendimentoService.atualizar(id, atendimento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        atendimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
