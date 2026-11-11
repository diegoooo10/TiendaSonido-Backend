package producto_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import producto_service.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    Optional<Producto> findByCodigo(String codigo);

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    Optional<Producto> findByCodigoAndActivoTrue(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByCategoriaId(Long categoriaId);

    long countByCategoriaId(Long categoriaId);

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    List<Producto> findByActivoTrueAndCategoriaIdOrderByNombreAsc(Long categoriaId);

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    List<Producto> findByActivoTrueAndEnOfertaTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    @Query("""
            select p from Producto p
            where p.activo = true
              and (:q is null
                   or lower(p.nombre) like lower(concat('%', :q, '%'))
                   or lower(p.codigo) like lower(concat('%', :q, '%'))
                   or lower(p.marca) like lower(concat('%', :q, '%'))
                   or lower(p.modelo) like lower(concat('%', :q, '%')))
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:marca is null or lower(p.marca) = lower(:marca))
              and (:enOferta is null or p.enOferta = :enOferta)
            order by p.nombre asc
            """)
    List<Producto> buscar(
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("marca") String marca,
            @Param("enOferta") Boolean enOferta
    );

    @EntityGraph(attributePaths = { "categoria", "inventario" })
    @Query("""
            select p from Producto p
            where p.activo = true
              and (:q is null
                   or lower(p.nombre) like lower(concat('%', :q, '%'))
                   or lower(p.codigo) like lower(concat('%', :q, '%'))
                   or lower(p.marca) like lower(concat('%', :q, '%'))
                   or lower(p.modelo) like lower(concat('%', :q, '%')))
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:marca is null or lower(p.marca) = lower(:marca))
              and (:enOferta is null or p.enOferta = :enOferta)
            """)
    Page<Producto> buscarPaginado(
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("marca") String marca,
            @Param("enOferta") Boolean enOferta,
            Pageable pageable
    );
}
