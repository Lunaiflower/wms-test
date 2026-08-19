package com.wms.repository;

import com.wms.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存 Repository — 候选人需要实现库存查询（任务2）
 * 提示：你可能需要添加自定义查询方法
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    boolean existsByProductId(Long productId);

    @Query(value = """
            SELECT i.productId AS productId, p.name AS productName, p.sku AS sku,
                   i.locationCode AS locationCode, w.name AS warehouseName,
                   i.quantity AS quantity, i.updatedAt AS updatedAt
            FROM Inventory i
            JOIN i.product p
            JOIN i.location l
            JOIN l.warehouse w
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
            """,
            countQuery = """
            SELECT COUNT(i)
            FROM Inventory i
            JOIN i.product p
            JOIN i.location l
            JOIN l.warehouse w
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
            """)
    Page<InventoryProjection> searchInventory(@Param("keyword") String keyword,
                                               @Param("warehouseId") Long warehouseId,
                                               Pageable pageable);
}
