package producto_service.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventario")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "stock_fisico", nullable = false)
    private Integer stockFisico = 0;

    @Column(name = "stock_reservado", nullable = false)
    private Integer stockReservado = 0;

    @Column(name = "stock_critico", nullable = false)
    private Integer stockCritico = 3;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * El stock disponible no se persiste: siempre se calcula a partir del stock fisico
     * menos las unidades reservadas por pedidos.
     */
    public int getStockDisponible() {
        int fisico = stockFisico == null ? 0 : stockFisico;
        int reservado = stockReservado == null ? 0 : stockReservado;
        return Math.max(0, fisico - reservado);
    }

    public boolean isBajoStockCritico() {
        int critico = stockCritico == null ? 0 : stockCritico;
        return getStockDisponible() <= critico;
    }

    @PrePersist
    public void prePersist() {
        this.fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);
        if (this.stockFisico == null) {
            this.stockFisico = 0;
        }
        if (this.stockReservado == null) {
            this.stockReservado = 0;
        }
        if (this.stockCritico == null) {
            this.stockCritico = 3;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);
    }
}
