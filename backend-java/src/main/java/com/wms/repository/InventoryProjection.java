package com.wms.repository;

import java.time.LocalDateTime;

/**
 * 库存列表查询的扁平投影，避免将关联实体逐行加载造成 N+1 查询。
 */
public interface InventoryProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    String getLocationCode();
    String getWarehouseName();
    Integer getQuantity();
    LocalDateTime getUpdatedAt();
}
