package producto_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import producto_service.model.ReservaStock;

public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

    @EntityGraph(attributePaths = { "producto" })
    Optional<ReservaStock> findByReferencia(String referencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "producto" })
    @Query("select r from ReservaStock r where r.referencia = :referencia")
    Optional<ReservaStock> findByReferenciaForUpdate(@Param("referencia") String referencia);

    @EntityGraph(attributePaths = { "producto" })
    List<ReservaStock> findByPedidoIdOrderByFechaCreacionAsc(String pedidoId);

    boolean existsByReferencia(String referencia);
}
