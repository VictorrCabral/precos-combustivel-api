package br.com.victorcabral.precos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agregado semanal por municipio e produto. A API le somente esta tabela:
 * a de dado bruto (preco_coleta) fica para o carregador e para auditoria.
 */
@Entity
@Table(name = "resumo_semanal")
public class ResumoSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    @Column(name = "semana_fim", nullable = false)
    private LocalDate semanaFim;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false, length = 120)
    private String municipio;

    @Column(nullable = false, length = 60)
    private String produto;

    @Column(name = "preco_medio", nullable = false)
    private BigDecimal precoMedio;

    @Column(name = "preco_minimo", nullable = false)
    private BigDecimal precoMinimo;

    @Column(name = "preco_maximo", nullable = false)
    private BigDecimal precoMaximo;

    @Column(name = "postos_pesquisados", nullable = false)
    private Integer postosPesquisados;

    protected ResumoSemanal() {
        // exigido pelo JPA
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSemanaInicio() {
        return semanaInicio;
    }

    public LocalDate getSemanaFim() {
        return semanaFim;
    }

    public String getUf() {
        return uf;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getProduto() {
        return produto;
    }

    public BigDecimal getPrecoMedio() {
        return precoMedio;
    }

    public BigDecimal getPrecoMinimo() {
        return precoMinimo;
    }

    public BigDecimal getPrecoMaximo() {
        return precoMaximo;
    }

    public Integer getPostosPesquisados() {
        return postosPesquisados;
    }
}
