package br.com.victorcabral.precos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextoRendererTest {

    @Test
    @DisplayName("o menor valor vira a barra mais baixa e o maior a mais alta")
    void sparklineUsaAFaixaInteira() {
        List<BigDecimal> valores = List.of(
                new BigDecimal("6.00"), new BigDecimal("6.50"), new BigDecimal("7.00"));

        String desenho = TextoRenderer.sparkline(valores);

        assertEquals(3, desenho.length());
        assertEquals('▁', desenho.charAt(0));
        assertEquals('█', desenho.charAt(2));
    }

    @Test
    @DisplayName("serie sem variacao nao pode dividir por zero")
    void sparklineComValoresIguais() {
        List<BigDecimal> iguais = List.of(new BigDecimal("6.99"), new BigDecimal("6.99"));

        assertEquals("▁▁", TextoRenderer.sparkline(iguais));
    }

    @Test
    @DisplayName("variacao de centavos nao vira grafico dramatico")
    void sparklineIgnoraRuidoDeCentavo() {
        List<BigDecimal> quaseIguais = List.of(
                new BigDecimal("6.98"), new BigDecimal("6.99"), new BigDecimal("6.99"));

        assertEquals("▁▁▁", TextoRenderer.sparkline(quaseIguais));
    }

    @Test
    void capitalizaNomeComposto() {
        assertEquals("Sao Paulo", TextoRenderer.capitalizar("SAO PAULO"));
        assertEquals("Manaus", TextoRenderer.capitalizar("MANAUS"));
        assertEquals("Gasolina Aditivada", TextoRenderer.capitalizar("GASOLINA ADITIVADA"));
    }
}
