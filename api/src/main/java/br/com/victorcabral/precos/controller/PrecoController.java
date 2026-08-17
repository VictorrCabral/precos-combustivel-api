package br.com.victorcabral.precos.controller;

import br.com.victorcabral.precos.dto.PrecoResponse;
import br.com.victorcabral.precos.service.PrecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@Validated
@Tag(name = "Precos", description = "Precos de combustiveis por municipio, a partir do levantamento semanal da ANP")
public class PrecoController {

    private final PrecoService service;

    public PrecoController(PrecoService service) {
        this.service = service;
    }

    @Operation(summary = "Preco medio mais recente de um combustivel no municipio")
    @GetMapping("/precos")
    public PrecoResponse precoAtual(
            @RequestParam @NotBlank String uf,
            @RequestParam @NotBlank String municipio,
            @RequestParam(defaultValue = "gasolina") String produto) {
        return service.precoAtual(uf, municipio, produto);
    }

    @Operation(summary = "Serie historica semanal, para montar grafico")
    @GetMapping("/precos/historico")
    public List<PrecoResponse> historico(
            @RequestParam @NotBlank String uf,
            @RequestParam @NotBlank String municipio,
            @RequestParam(defaultValue = "gasolina") String produto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.historico(uf, municipio, produto, de, ate);
    }

    @Operation(summary = "Municipios com dado disponivel na UF")
    @GetMapping("/municipios")
    public List<String> municipios(@RequestParam @NotBlank String uf) {
        return service.municipios(uf);
    }

    @Operation(summary = "Produtos disponiveis")
    @GetMapping("/produtos")
    public List<String> produtos() {
        return service.produtos();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(PrecoService.DadoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(PrecoService.DadoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", e.getMessage()));
    }
}
