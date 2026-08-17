package br.com.victorcabral.precos.service;

import br.com.victorcabral.precos.dto.PrecoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Monta a saida em texto puro, para quem consulta a API pelo terminal com curl.
 * Inspirado no wttr.in: dev nenhum quer abrir navegador para ver um numero.
 */
@Component
public class TextoRenderer {

    private static final char[] BARRAS = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    public String render(PrecoResponse atual, List<PrecoResponse> historico) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(BR);
        StringBuilder saida = new StringBuilder("\n");

        saida.append("  ")
             .append(capitalizar(atual.produto()))
             .append(" em ")
             .append(capitalizar(atual.municipio()))
             .append(" (").append(atual.uf()).append(")\n");

        saida.append("  semana de ").append(atual.semanaInicio().format(DIA))
             .append(" a ").append(atual.semanaFim().format(DIA))
             .append("\n\n");

        saida.append("  Média    ").append(moeda.format(atual.precoMedio())).append('\n');
        saida.append("  Mínimo   ").append(moeda.format(atual.precoMinimo())).append('\n');
        saida.append("  Máximo   ").append(moeda.format(atual.precoMaximo())).append('\n');
        saida.append("  Postos   ").append(atual.postosPesquisados()).append('\n');

        // O historico vem com todos os produtos que casam com o prefixo pedido
        // (gasolina traz tambem a aditivada). No grafico queremos uma serie so.
        List<PrecoResponse> serie = historico.stream()
                .filter(p -> p.produto().equals(atual.produto()))
                .toList();

        if (serie.size() >= 2) {
            List<BigDecimal> valores = serie.stream().map(PrecoResponse::precoMedio).toList();
            saida.append('\n')
                 .append("  ").append(sparkline(valores)).append('\n')
                 .append("  ").append(serie.get(0).semanaInicio().format(DIA))
                 .append(" a ").append(serie.get(serie.size() - 1).semanaFim().format(DIA))
                 .append("   ").append(moeda.format(menor(valores)))
                 .append(" a ").append(moeda.format(maior(valores)))
                 .append('\n');
        }

        saida.append('\n')
             .append("  fonte: ANP  ·  json: /v1/precos?uf=")
             .append(atual.uf()).append("&municipio=").append(atual.municipio())
             .append("\n\n");

        return saida.toString();
    }

    /** Abaixo disso a variacao e ruido de centavo, nao movimento de preco. */
    private static final double VARIACAO_MINIMA = 0.05;

    static String sparkline(List<BigDecimal> valores) {
        double minimo = valores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);
        double maximo = valores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double amplitude = maximo - minimo;

        // Normalizar pelo minimo e maximo transforma um centavo de diferenca em
        // um grafico dramatico. Se a serie mal se mexeu, ela e desenhada plana.
        if (amplitude < VARIACAO_MINIMA) {
            return String.valueOf(BARRAS[0]).repeat(valores.size());
        }

        StringBuilder desenho = new StringBuilder();
        for (BigDecimal valor : valores) {
            int nivel = amplitude == 0
                    ? 0
                    : (int) Math.round((valor.doubleValue() - minimo) / amplitude * (BARRAS.length - 1));
            desenho.append(BARRAS[nivel]);
        }
        return desenho.toString();
    }

    static String capitalizar(String texto) {
        return Arrays.stream(texto.toLowerCase(BR).split(" "))
                .map(parte -> parte.isEmpty()
                        ? parte
                        : Character.toUpperCase(parte.charAt(0)) + parte.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static BigDecimal menor(List<BigDecimal> valores) {
        return valores.stream().min(BigDecimal::compareTo).orElseThrow();
    }

    private static BigDecimal maior(List<BigDecimal> valores) {
        return valores.stream().max(BigDecimal::compareTo).orElseThrow();
    }
}
