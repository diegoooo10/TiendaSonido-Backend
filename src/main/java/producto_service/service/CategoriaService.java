package producto_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import producto_service.dto.CategoriaRequestDTO;
import producto_service.dto.CategoriaResponseDTO;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.mapper.CategoriaMapper;
import producto_service.model.Categoria;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.ProductoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {
        log.info("Listando categorias");
        return categoriaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(CategoriaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO obtenerPorId(Long id) {
        log.info("Buscando categoria {}", id);
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        return CategoriaMapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponseDTO guardar(CategoriaRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        log.info("Creando categoria {}", nombre);

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoria con ese nombre");
        }

        Categoria categoria = CategoriaMapper.toEntity(dto);
        categoria.setNombre(nombre);
        categoria.setActivo(dto.getActivo() == null || dto.getActivo());

        Categoria guardada = categoriaRepository.save(categoria);
        log.info("Categoria {} creada con id {}", nombre, guardada.getId());
        return CategoriaMapper.toResponse(guardada);
    }

    @Transactional
    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        log.info("Actualizando categoria {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoria con ese nombre");
        }

        CategoriaMapper.aplicar(dto, categoria);
        categoria.setNombre(nombre);
        if (dto.getActivo() != null) {
            categoria.setActivo(dto.getActivo());
        }

        Categoria actualizada = categoriaRepository.save(categoria);
        log.info("Categoria {} actualizada", id);
        return CategoriaMapper.toResponse(actualizada);
    }

    /**
     * Solo se elimina fisicamente una categoria sin productos asociados.
     * Si tiene productos (activos o inactivos), se rechaza la operacion.
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando categoria {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

        if (productoRepository.countByCategoriaId(id) > 0) {
            throw new ConflictException("No se puede eliminar una categoria con productos asociados");
        }

        categoriaRepository.delete(categoria);
        log.info("Categoria {} eliminada", id);
    }
}
