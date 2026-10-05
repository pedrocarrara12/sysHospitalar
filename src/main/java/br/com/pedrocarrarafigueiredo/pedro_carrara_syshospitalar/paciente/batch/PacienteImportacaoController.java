package br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch;

import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteInicioResponse;
import br.com.pedrocarrarafigueiredo.pedro_carrara_syshospitalar.paciente.batch.dto.ImportacaoPacienteStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/batch/pacientes/importacoes")
@Tag(name = "Importacao de pacientes", description = "Processamento em lote de pacientes por CSV")
public class PacienteImportacaoController {
    private final PacienteImportacaoService importacaoService;

    public PacienteImportacaoController(PacienteImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Inicia uma importacao assíncrona de pacientes")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Importacao aceita"),
            @ApiResponse(responseCode = "400", description = "Arquivo CSV invalido"),
            @ApiResponse(responseCode = "409", description = "Ja existe importacao em andamento")
    })
    public ResponseEntity<ImportacaoPacienteInicioResponse> iniciar(
            @RequestPart("arquivo") MultipartFile arquivo) {
        ImportacaoPacienteInicioResponse response = importacaoService.iniciar(arquivo);
        URI location = URI.create("/batch/pacientes/importacoes/" + response.executionId());
        return ResponseEntity.accepted().location(location).body(response);
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Consulta o status de uma importacao")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status encontrado"),
            @ApiResponse(responseCode = "404", description = "Execucao nao encontrada")
    })
    public ResponseEntity<ImportacaoPacienteStatusResponse> buscarStatus(@PathVariable long executionId) {
        return ResponseEntity.ok(importacaoService.buscarStatus(executionId));
    }
}
