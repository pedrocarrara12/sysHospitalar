package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Enfermeiro;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.EnfermeiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @ApiResponse(responseCode = "200", description = "Enfermeiros listados com sucesso")
    public ResponseEntity<List<Enfermeiro>> buscarTodos() {
        return ResponseEntity.ok(enfermeiroService.buscarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um enfermeiro por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enfermeiro encontrado"),
            @ApiResponse(responseCode = "404", description = "Enfermeiro nao encontrado")
    })
    public ResponseEntity<Enfermeiro> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(enfermeiroService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um enfermeiro")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Enfermeiro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos")
    })
    public ResponseEntity<Enfermeiro> inserir(@RequestBody @Valid Enfermeiro enfermeiro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enfermeiroService.cadastrar(enfermeiro));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um enfermeiro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enfermeiro atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Enfermeiro nao encontrado")
    })
    public ResponseEntity<Enfermeiro> atualizar(@PathVariable Long id, @RequestBody @Valid Enfermeiro enfermeiro) {
        return ResponseEntity.ok(enfermeiroService.atualizar(id, enfermeiro));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um enfermeiro")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Enfermeiro removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Enfermeiro nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        enfermeiroService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
