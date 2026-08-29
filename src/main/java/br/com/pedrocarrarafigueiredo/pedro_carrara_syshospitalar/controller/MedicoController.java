package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.domain.Medico;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.service.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicos")
@Tag(name = "Médicos", description = "Endpoints para gerenciamento de médicos")
public class MedicoController {
    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os médicos")
    @ApiResponse(responseCode = "200", description = "Medicos listados com sucesso")
    public ResponseEntity<List<Medico>> buscarTodos() {
        return ResponseEntity.ok(medicoService.buscarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um médico por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medico encontrado"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<Medico> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Medico cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "Medico ja cadastrado")
    })
    public ResponseEntity<Medico> inserir(@RequestBody Medico medico) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.cadastrar(medico));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Medico atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<Medico> atualizar(@PathVariable Long id, @RequestBody Medico medico) {
        return ResponseEntity.ok(medicoService.atualizar(id, medico));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um médico")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Medico removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medico nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        medicoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
