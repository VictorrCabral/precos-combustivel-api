package br.com.victorcabral.precos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.victorcabral.precos.dto.PrecoResponse;
import br.com.victorcabral.precos.service.PrecoService;
import br.com.victorcabral.precos.service.TextoRenderer;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@Hidden
public class TextoController {

    private final PrecoService service;
    private final TextoRenderer renderer;

    public TextoController(PrecoService service, TextoRenderer renderer) {
        this.service = service;
        this.renderer = renderer;
    }

    @GetMapping(
            value = {"/{uf:[A-Za-z]{2}}/{municipio}", "/{uf:[A-Za-z]{2}}/{municipio}/{produto}"},
            produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> terminal(@PathVariable String uf,
                                           @PathVariable String municipio,
                                           @PathVariable(required = false) String produto) {
        String combustivel = (produto == null || produto.isBlank()) ? "gasolina" : produto;
        try {
            PrecoResponse atual = service.precoAtual(uf, municipio, combustivel);
            List<PrecoResponse> historico = service.historico(uf, municipio, combustivel, null, null);
            return ResponseEntity.ok(renderer.render(atual, historico));
        } catch (PrecoService.DadoNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("\n  " + e.getMessage() + "\n\n  tente: curl .../AM/Manaus\n\n");
        }
    }
}
