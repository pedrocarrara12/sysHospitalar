package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Paciente;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pacientes")
@Tag(name = "Pacientes", description = "Endpoints para gerenciamento de pacientes")
public class PacienteController {
    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }
    @GetMapping
    @Operation(summary = "Lista todos os pacientes")
    public ResponseEntity<List<Paciente>> buscarTodos() {
        return ResponseEntity.ok(pacienteService.buscarTodos());
    }
    @GetMapping("/{id}")
    @Operation(summary = "Busca um paciente por ID")
    public ResponseEntity<Paciente> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }
    @PostMapping
    @Operation(summary = "Cadastra um paciente")
    public ResponseEntity<Paciente> inserir(@RequestBody Paciente paciente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.cadastrar(paciente));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um paciente")
    public ResponseEntity<Paciente> atualizar(@PathVariable Long id, @RequestBody Paciente paciente) {
        return ResponseEntity.ok(pacienteService.atualizar(id,paciente));
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um paciente")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        pacienteService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
