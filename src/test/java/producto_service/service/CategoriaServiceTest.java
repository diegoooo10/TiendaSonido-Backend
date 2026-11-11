package producto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import producto_service.dto.CategoriaRequestDTO;
import producto_service.dto.CategoriaResponseDTO;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.model.Categoria;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categoriaRepository, productoRepository);
    }

    @Test
    @DisplayName("listar categorias")
    void listarCategorias() {
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(categoria()));

        List<CategoriaResponseDTO> resultado = categoriaService.listar();

        assertEquals(1, resultado.size());
        assertEquals("Guitarras Acusticas", resultado.get(0).getNombre());
        assertTrue(resultado.get(0).getActivo());
    }

    @Test
    @DisplayName("obtener categoria inexistente retorna 404")
    void obtenerCategoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("crear categoria correctamente")
    void crearCategoria() {
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("  Accesorios  ");
        dto.setDescripcion("Accesorios");
        dto.setActivo(true);

        when(categoriaRepository.existsByNombreIgnoreCase("Accesorios")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocacion -> {
            Categoria categoria = invocacion.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });

        CategoriaResponseDTO resultado = categoriaService.guardar(dto);

        assertEquals("Accesorios", resultado.getNombre());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("crear categoria duplicada retorna conflicto")
    void crearCategoriaDuplicada() {
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Accesorios");

        when(categoriaRepository.existsByNombreIgnoreCase("Accesorios")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoriaService.guardar(dto));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    @DisplayName("actualizar categoria correctamente")
    void actualizarCategoria() {
        Categoria categoria = categoria();
        CategoriaRequestDTO dto = new CategoriaRequestDTO();
        dto.setNombre("Accesorios");
        dto.setDescripcion("Nueva");
        dto.setActivo(false);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Accesorios", 1L)).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoriaResponseDTO resultado = categoriaService.actualizar(1L, dto);

        assertEquals("Accesorios", resultado.getNombre());
        assertFalse(resultado.getActivo());
    }

    @Test
    @DisplayName("no eliminar categoria con productos asociados")
    void noEliminarCategoriaConProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(productoRepository.countByCategoriaId(1L)).thenReturn(3L);

        assertThrows(ConflictException.class, () -> categoriaService.eliminar(1L));
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

    @Test
    @DisplayName("eliminar categoria sin productos")
    void eliminarCategoriaSinProductos() {
        Categoria categoria = categoria();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.countByCategoriaId(1L)).thenReturn(0L);

        categoriaService.eliminar(1L);

        verify(categoriaRepository).delete(categoria);
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Guitarras Acusticas");
        categoria.setDescripcion("Categoria");
        categoria.setActivo(true);
        return categoria;
    }
}
