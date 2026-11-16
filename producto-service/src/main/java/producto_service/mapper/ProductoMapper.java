package producto_service.mapper;

import java.math.BigDecimal;

import producto_service.dto.ProductoRequestDTO;
import producto_service.dto.ProductoResponseDTO;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static Producto toEntity(ProductoRequestDTO dto, Categoria categoria) {
        Producto producto = new Producto();
        aplicar(dto, producto, categoria);
        return producto;
    }

    public static void aplicar(ProductoRequestDTO dto, Producto producto, Categoria categoria) {
        producto.setCodigo(dto.getCodigo());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setMarca(dto.getMarca());
        producto.setModelo(dto.getModelo());
        producto.setImagen(dto.getImagen());
        producto.setPrecio(dto.getPrecio());
        producto.setPrecioOferta(dto.getPrecioOferta());
        producto.setEnOferta(Boolean.TRUE.equals(dto.getEnOferta()));
        producto.setCategoria(categoria);
    }

    public static ProductoResponseDTO toResponse(Producto producto) {
        Inventario inventario = producto.getInventario();
        int stockDisponible = inventario == null ? 0 : inventario.getStockDisponible();
        boolean activo = Boolean.TRUE.equals(producto.getActivo());

        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .marca(producto.getMarca())
                .modelo(producto.getModelo())
                .imagen(producto.getImagen())
                .precio(producto.getPrecio())
                .precioOferta(producto.getPrecioOferta())
                .enOferta(Boolean.TRUE.equals(producto.getEnOferta()))
                .precioEfectivo(calcularPrecioEfectivo(producto))
                .activo(activo)
                .categoriaId(producto.getCategoria() == null ? null : producto.getCategoria().getId())
                .categoriaNombre(producto.getCategoria() == null ? null : producto.getCategoria().getNombre())
                .stockDisponible(stockDisponible)
                .stock(stockDisponible)
                .disponible(activo && stockDisponible > 0)
                .build();
    }

    /**
     * El backend es la fuente oficial del precio de compra. Un precio de oferta
     * almacenado no se aplica si el producto no esta marcado como en oferta.
     */
    public static BigDecimal calcularPrecioEfectivo(Producto producto) {
        if (Boolean.TRUE.equals(producto.getEnOferta()) && producto.getPrecioOferta() != null) {
            return producto.getPrecioOferta();
        }
        return producto.getPrecio();
    }
}
