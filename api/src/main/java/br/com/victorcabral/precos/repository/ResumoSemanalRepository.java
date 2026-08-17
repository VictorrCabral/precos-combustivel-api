package br.com.victorcabral.precos.repository;

import br.com.victorcabral.precos.domain.ResumoSemanal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ResumoSemanalRepository extends JpaRepository<ResumoSemanal, Long> {

    /**
     * Semana mais recente disponivel para o municipio e produto informados.
     * Usa Pageable em vez de LIMIT no JPQL: LIMIT so existe no HQL recente e
     * quebraria em outra versao do Hibernate. Pageable funciona em qualquer uma.
     */
    @Query("""
            SELECT r FROM ResumoSemanal r
            WHERE r.uf = :uf
              AND r.municipio = :municipio
              AND r.produto LIKE :produto
            ORDER BY r.semanaInicio DESC
            """)
    List<ResumoSemanal> buscarMaisRecente(@Param("uf") String uf,
                                          @Param("municipio") String municipio,
                                          @Param("produto") String produto,
                                          Pageable pageable);

    @Query("""
            SELECT r FROM ResumoSemanal r
            WHERE r.uf = :uf
              AND r.municipio = :municipio
              AND r.produto LIKE :produto
              AND r.semanaInicio BETWEEN :de AND :ate
            ORDER BY r.semanaInicio ASC
            """)
    List<ResumoSemanal> buscarHistorico(@Param("uf") String uf,
                                        @Param("municipio") String municipio,
                                        @Param("produto") String produto,
                                        @Param("de") LocalDate de,
                                        @Param("ate") LocalDate ate);

    @Query("""
            SELECT DISTINCT r.municipio FROM ResumoSemanal r
            WHERE r.uf = :uf
            ORDER BY r.municipio ASC
            """)
    List<String> listarMunicipios(@Param("uf") String uf);

    @Query("SELECT DISTINCT r.produto FROM ResumoSemanal r ORDER BY r.produto ASC")
    List<String> listarProdutos();
}
