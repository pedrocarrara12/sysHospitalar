package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.controller;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.request.AtendimentoRequest;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.dto.response.AtendimentoResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.StatusAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.enuns.TipoAtendimento;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.atendimento.service.AtendimentoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @ApiResponse(responseCode = "200", description = "Atendimentos listados com sucesso")
    public ResponseEntity<List<AtendimentoResponse>> buscarTodos() {
        return ResponseEntity.ok(atendimentoService.buscarTodos());
    }

    @GetMapping("/filtro/status")
    @Operation(summary = "Filtra atendimentos por status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimentos filtrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido")
    })
    public ResponseEntity<List<AtendimentoResponse>> filtrarPorStatus(@RequestParam StatusAtendimento status) {
        return ResponseEntity.ok(atendimentoService.filtrarPorStatus(status));
    }

    @GetMapping("/filtro/tipo")
    @Operation(summary = "Filtra atendimentos por tipo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimentos filtrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido")
    })
    public ResponseEntity<List<AtendimentoResponse>> filtrarPorTipo(@RequestParam TipoAtendimento tipo) {
        return ResponseEntity.ok(atendimentoService.filtrarPorTipo(tipo));
    }

    @GetMapping("/ordenados-por-data")
    @Operation(summary = "Lista atendimentos ordenados por data e hora")
    @ApiResponse(responseCode = "200", description = "Atendimentos ordenados com sucesso")
    public ResponseEntity<List<AtendimentoResponse>> listarOrdenadoPorDataHora() {
        return ResponseEntity.ok(atendimentoService.listarOrdenadoPorDataHora());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um atendimento por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento encontrado"),
            @ApiResponse(responseCode = "404", description = "Atendimento nao encontrado")
    })
    public ResponseEntity<AtendimentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um atendimento")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atendimento cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos")
    })
    public ResponseEntity<AtendimentoResponse> inserir(@RequestBody @Valid AtendimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atendimentoService.cadastrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um atendimento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Atendimento nao encontrado")
    })
    public ResponseEntity<AtendimentoResponse> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid AtendimentoRequest request) {
        return ResponseEntity.ok(atendimentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um atendimento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atendimento removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Atendimento nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        atendimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
