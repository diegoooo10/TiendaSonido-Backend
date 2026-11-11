package producto_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import producto_service.config.InventarioProperties;
import producto_service.dto.ProductoRequestDTO;
import producto_service.dto.ProductoResponseDTO;
import producto_service.exception.BusinessException;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.mapper.ProductoMapper;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final InventarioProperties inventarioProperties;

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listar(String q, Long categoriaId, String marca, Boolean enOferta) {
        log.info("Listando productos activos");
        return productoRepository.buscar(normalizarTexto(q), categoriaId, normalizarTexto(marca), enOferta)
                .stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponseDTO> listarPaginado(
            String q,
            Long categoriaId,
            String marca,
            Boolean enOferta,
            int page,
            int size
    ) {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(pagina, tamano, Sort.by("nombre").ascending());

        return productoRepository
                .buscarPaginado(normalizarTexto(q), categoriaId, normalizarTexto(marca), enOferta, pageable)
                .map(ProductoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorCodigo(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        log.info("Buscando producto activo con codigo {}", codigoNormalizado);

        Producto producto = productoRepository.findByCodigoAndActivoTrue(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        return ProductoMapper.toResponse(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarOfertas() {
        log.info("Listando productos en oferta");
        return productoRepository.findByActivoTrueAndEnOfertaTrueOrderByNombreAsc()
                .stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> buscarPorCategoria(Long categoriaId) {
        log.info("Buscando productos activos de la categoria {}", categoriaId);

        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("Categoria no encontrada");
        }

        return productoRepository.findByActivoTrueAndCategoriaIdOrderByNombreAsc(categoriaId)
                .stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductoResponseDTO guardar(ProductoRequestDTO dto) {
        String codigo = normalizarCodigo(dto.getCodigo());
        log.info("Creando producto con codigo {}", codigo);

        if (productoRepository.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un producto con el codigo " + codigo);
        }

        Categoria categoria = obtenerCategoriaActiva(dto.getCategoriaId());
        BigDecimal precio = validarPrecio(dto.getPrecio(), "precio");
        BigDecimal precioOferta = validarPrecioOferta(dto.getPrecioOferta(), precio, dto.getEnOferta());

        Producto producto = ProductoMapper.toEntity(dto, categoria);
        producto.setCodigo(codigo);
        producto.setPrecio(precio);
        producto.setPrecioOferta(precioOferta);
        producto.setActivo(dto.getActivo() == null || dto.getActivo());

        Producto guardado = productoRepository.save(producto);

        Inventario inventario = new Inventario();
        inventario.setProducto(guardado);
        inventario.setStockFisico(0);
        inventario.setStockReservado(0);
        inventario.setStockCritico(inventarioProperties.getStockCriticoInicial());
        Inventario inventarioGuardado = inventarioRepository.save(inventario);
        guardado.setInventario(inventarioGuardado);

        log.info("Producto {} creado con id {} y stock inicial 0", codigo, guardado.getId());
        return ProductoMapper.toResponse(guardado);
    }

    @Transactional
    public ProductoResponseDTO actualizar(String codigo, ProductoRequestDTO dto) {
        String codigoNormalizado = normalizarCodigo(codigo);
        log.info("Actualizando producto {}", codigoNormalizado);

        Producto existente = productoRepository.findByCodigo(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        String codigoEnviado = normalizarCodigo(dto.getCodigo());
        if (!codigoNormalizado.equals(codigoEnviado)) {
            throw new BusinessException("El codigo del cuerpo debe coincidir con el codigo de la ruta");
        }

        Categoria categoria = obtenerCategoriaActiva(dto.getCategoriaId());
        BigDecimal precio = validarPrecio(dto.getPrecio(), "precio");
        BigDecimal precioOferta = validarPrecioOferta(dto.getPrecioOferta(), precio, dto.getEnOferta());

        ProductoMapper.aplicar(dto, existente, categoria);
        existente.setCodigo(codigoNormalizado);
        existente.setPrecio(precio);
        existente.setPrecioOferta(precioOferta);
        if (dto.getActivo() != null) {
            existente.setActivo(dto.getActivo());
        }

        Producto actualizado = productoRepository.save(existente);
        log.info("Producto {} actualizado correctamente", codigoNormalizado);
        return ProductoMapper.toResponse(actualizado);
    }

    /**
     * Baja logica: el producto se conserva en la base de datos y deja de
     * mostrarse en el catalogo publico.
     */
    @Transactional
    public void desactivar(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        log.info("Desactivando producto {}", codigoNormalizado);

        Producto producto = productoRepository.findByCodigo(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (Boolean.TRUE.equals(producto.getActivo())) {
            producto.setActivo(false);
            productoRepository.save(producto);
            log.info("Producto {} desactivado", codigoNormalizado);
        }
    }

    private Categoria obtenerCategoriaActiva(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new BusinessException("La categoria seleccionada no esta activa");
        }

        return categoria;
    }

    private BigDecimal validarPrecio(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new BusinessException("El " + campo + " debe ser mayor a 0");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("El " + campo + " admite como maximo 2 decimales");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal validarPrecioOferta(BigDecimal precioOferta, BigDecimal precio, Boolean enOferta) {
        if (Boolean.TRUE.equals(enOferta) && precioOferta == null) {
            throw new BusinessException("Un producto en oferta debe tener precioOferta");
        }

        if (precioOferta == null) {
            return null;
        }

        BigDecimal oferta = validarPrecio(precioOferta, "precioOferta");
        if (oferta.compareTo(precio) >= 0) {
            throw new BusinessException("El precioOferta debe ser menor al precio normal");
        }
        return oferta;
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El codigo es obligatorio");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
