package com.gila.api.ecommerce.repository.product;

import com.gila.api.ecommerce.configuration.cache.CacheNames;
import com.gila.api.ecommerce.model.query.ProductUpsertRow;
import com.gila.api.ecommerce.model.query.UpsertOutcome;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProductBatchRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO products (sku, name, description, category_id, price, stock, weight_kg)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (sku) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                category_id = EXCLUDED.category_id,
                price = EXCLUDED.price,
                stock = EXCLUDED.stock,
                weight_kg = EXCLUDED.weight_kg,
                is_active = TRUE,
                deleted_at = NULL,
                version = products.version + 1
            """;

    private static final String EXISTING_SKUS_SQL = "SELECT sku FROM products WHERE sku IN (:skus)";

    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;

    public ProductBatchRepository(JdbcTemplate jdbcTemplate, JdbcClient jdbcClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = jdbcClient;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, allEntries = true,
                    condition = "#result.updated() > 0"),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_QUERY, allEntries = true)})
    @Transactional
    public UpsertOutcome upsertBatch(List<ProductUpsertRow> rows) {
        if (rows.isEmpty()) {
            return UpsertOutcome.EMPTY;
        }
        Set<String> knownSkus = new HashSet<>(findExistingSkus(rows));
        long updated = 0;
        for (ProductUpsertRow row : rows) {
            if (!knownSkus.add(row.sku())) {
                updated++;
            }
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, rows, rows.size(), (ps, row) -> {
            ps.setString(1, row.sku());
            ps.setString(2, row.name());
            ps.setObject(3, row.description(), Types.VARCHAR);
            ps.setShort(4, row.categoryId());
            ps.setBigDecimal(5, row.price());
            ps.setInt(6, row.stock());
            ps.setObject(7, row.weightKg(), Types.NUMERIC);
        });
        return new UpsertOutcome(rows.size() - updated, updated);
    }

    private List<String> findExistingSkus(List<ProductUpsertRow> rows) {
        List<String> skus = rows.stream().map(ProductUpsertRow::sku).distinct().toList();
        return jdbcClient.sql(EXISTING_SKUS_SQL).param("skus", skus).query(String.class).list();
    }
}
