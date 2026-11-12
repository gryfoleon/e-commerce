package com.gila.api.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Getter
    @Column(name = "sku", nullable = false, updatable = false, length = 10)
    private String sku;

    @Getter
    @Column(name = "name", nullable = false)
    private String name;

    @Getter
    @Column(name = "description")
    private @Nullable String description;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Getter
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Getter
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Getter
    @Column(name = "weight_kg", precision = 8, scale = 3)
    private @Nullable BigDecimal weightKg;

    @Getter
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Getter
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Getter
    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    @Getter
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public static Product create(
            String sku, String name, @Nullable String description, ProductCategory category,
            BigDecimal price, Integer stock, @Nullable BigDecimal weightKg) {
        Product product = new Product();
        product.sku = Objects.requireNonNull(sku, "sku must not be null");
        product.replaceDetails(name, description, category, price, stock, weightKg);
        return product;
    }

    public void replaceDetails(
            String name, @Nullable String description, ProductCategory category,
            BigDecimal price, Integer stock, @Nullable BigDecimal weightKg) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.stock = Objects.requireNonNull(stock, "stock must not be null");
        this.weightKg = weightKg;
    }

    public void softDelete(Instant when) {
        this.active = false;
        this.deletedAt = Objects.requireNonNull(when, "when must not be null");
    }

}
