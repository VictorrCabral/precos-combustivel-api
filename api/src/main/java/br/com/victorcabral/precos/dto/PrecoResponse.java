package br.com.victorcabral.precos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.victorcabral.precos.domain.ResumoSemanal;

public record PrecoResponse(
        String uf,
        String municipio,
        String produto,
        LocalDate semanaInicio,
        LocalDate semanaFim,
        BigDecimal precoMedio,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo,
        Integer postosPesquisados
) {
    public static PrecoResponse de(ResumoSemanal r) {
        return new PrecoResponse(
                r.getUf(),
                r.getMunicipio(),
                r.getProduto(),
                r.getSemanaInicio(),
                r.getSemanaFim(),
                r.getPrecoMedio(),
                r.getPrecoMinimo(),
                r.getPrecoMaximo(),
                r.getPostosPesquisados()
        );
    }
}
