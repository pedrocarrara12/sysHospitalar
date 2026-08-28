package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.EnfermeiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enfermeiros")
@Tag(name = "Enfermeiros", description = "Endpoints para gerenciamento de enfermeiros")
public class EnfermeiroController {
    private final EnfermeiroService enfermeiroService;

    public EnfermeiroController(EnfermeiroService enfermeiroService) {
        this.enfermeiroService = enfermeiroService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os enfermeiros")
    public ResponseEntity<List<Enfermeiro>> buscarTodos() {
        return ResponseEntity.ok(enfermeiroService.buscarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um enfermeiro por ID")
    public ResponseEntity<Enfermeiro> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(enfermeiroService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um enfermeiro")
    public ResponseEntity<Enfermeiro> inserir(@RequestBody Enfermeiro enfermeiro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enfermeiroService.cadastrar(enfermeiro));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um enfermeiro")
    public ResponseEntity<Enfermeiro> atualizar(@PathVariable Long id, @RequestBody Enfermeiro enfermeiro) {
        return ResponseEntity.ok(enfermeiroService.atualizar(id, enfermeiro));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um enfermeiro")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        enfermeiroService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
