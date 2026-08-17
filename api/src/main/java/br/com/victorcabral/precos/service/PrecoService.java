package br.com.victorcabral.precos.service;

import br.com.victorcabral.precos.dto.PrecoResponse;
import br.com.victorcabral.precos.repository.ResumoSemanalRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class PrecoService {

    private final ResumoSemanalRepository repository;

    public PrecoService(ResumoSemanalRepository repository) {
        this.repository = repository;
    }

    /**
     * O carregador grava municipio sem acento e em caixa alta. Fazemos o mesmo
     * com o que chega na URL, para "São Paulo", "sao paulo" e "SAO PAULO"
     * encontrarem a mesma cidade.
     */
    static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).trim();
    }

    /** "gasolina" casa com "GASOLINA" e "GASOLINA ADITIVADA". */
    private static String comoFiltro(String produto) {
        return normalizar(produto) + "%";
    }

    @Cacheable("precoAtual")
    public PrecoResponse precoAtual(String uf, String municipio, String produto) {
        return repository
                .buscarMaisRecente(normalizar(uf), normalizar(municipio), comoFiltro(produto),
                        PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(PrecoResponse::de)
                .orElseThrow(() -> new DadoNaoEncontradoException(
                        "Sem dado para %s/%s (%s)".formatted(municipio, uf, produto)));
    }

    @Cacheable("historico")
    public List<PrecoResponse> historico(String uf, String municipio, String produto,
                                         LocalDate de, LocalDate ate) {
        LocalDate inicio = de != null ? de : LocalDate.now().minusYears(1);
        LocalDate fim = ate != null ? ate : LocalDate.now();
        return repository
                .buscarHistorico(normalizar(uf), normalizar(municipio), comoFiltro(produto), inicio, fim)
                .stream()
                .map(PrecoResponse::de)
                .toList();
    }

    @Cacheable("municipios")
    public List<String> municipios(String uf) {
        return repository.listarMunicipios(normalizar(uf));
    }

    @Cacheable("produtos")
    public List<String> produtos() {
        return repository.listarProdutos();
    }

    public static class DadoNaoEncontradoException extends RuntimeException {
        public DadoNaoEncontradoException(String mensagem) {
            super(mensagem);
        }
    }
}
