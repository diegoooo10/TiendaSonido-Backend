package producto_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import producto_service.model.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    @EntityGraph(attributePaths = { "producto" })
    @Query("select i from Inventario i order by i.producto.nombre asc")
    List<Inventario> findAllConProducto();

    @EntityGraph(attributePaths = { "producto" })
    @Query("select i from Inventario i where i.producto.codigo = :codigo")
    Optional<Inventario> findByProductoCodigo(@Param("codigo") String codigo);

    /**
     * Bloqueo pesimista de la fila de inventario. Serializa las reservas,
     * confirmaciones y liberaciones del mismo producto para evitar sobreventa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "producto" })
    @Query("select i from Inventario i where i.producto.codigo = :codigo")
    Optional<Inventario> findByProductoCodigoForUpdate(@Param("codigo") String codigo);

    Optional<Inventario> findByProductoId(Long productoId);

    boolean existsByProductoId(Long productoId);
}
