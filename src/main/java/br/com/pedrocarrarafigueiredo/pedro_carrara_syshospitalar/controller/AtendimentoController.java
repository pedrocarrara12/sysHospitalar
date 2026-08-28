package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Atendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.AtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atendimentos")
@Tag(name = "Atendimentos", description = "Endpoints para gerenciamento de atendimentos")
public class AtendimentoController {
    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os atendimentos")
    public ResponseEntity<List<Atendimento>> buscarTodos() {
        return ResponseEntity.ok(atendimentoService.buscarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um atendimento por ID")
    public ResponseEntity<Atendimento> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um atendimento")
    public ResponseEntity<Atendimento> inserir(@RequestBody Atendimento atendimento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atendimentoService.cadastrar(atendimento));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um atendimento")
    public ResponseEntity<Atendimento> atualizar(@PathVariable Long id, @RequestBody Atendimento atendimento) {
        return ResponseEntity.ok(atendimentoService.atualizar(id, atendimento));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um atendimento")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        atendimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
