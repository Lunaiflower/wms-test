package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.InboundItemRequest;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderItemResponse;
import com.wms.dto.InboundOrderResponse;
import com.wms.dto.InventoryResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================
 *  候选人需要实现以下两个方法：
 * ============================================
 *
 * 1. createInboundOrder() — 入库单创建（任务1）
 *    要求：
 *    - 生成入库单号（格式 IN-YYYYMMDD-XXX）
 *    - 校验商品和库位是否存在
 *    - 在事务中同时创建入库单和更新库存
 *    - 参数校验已在 DTO 层通过 @Valid 处理
 *
 * 2. queryInventory() — 库存查询（任务2）
 *    要求：
 *    - 支持按商品名称/SKU模糊搜索
 *    - 支持按仓库筛选
 *    - 支持分页
 *    - 返回关联的商品名称和仓库名称
 *    - 注意性能：使用 JOIN 查询而非 N+1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final InventoryRepository inventoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    /**
     * 入库单创建 — 候选人实现
     */
    @Transactional
    public InboundOrderResponse createInboundOrder(InboundOrderCreateRequest request) {
        Map<Long, Product> products = validateProducts(request.getItems());
        validateLocations(request.getItems());

        InboundOrder order = inboundOrderRepository.save(InboundOrder.builder()
                .orderNo(generateOrderNo())
                .supplierName(request.getSupplierName().trim())
                .status("COMPLETED")
                .build());

        List<InboundOrderItem> orderItems = new ArrayList<>();
        List<InboundOrderItemResponse> itemResponses = new ArrayList<>();
        for (InboundItemRequest itemRequest : request.getItems()) {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndLocationCode(itemRequest.getProductId(), itemRequest.getLocationCode())
                    .orElseGet(() -> Inventory.builder()
                            .productId(itemRequest.getProductId())
                            .locationCode(itemRequest.getLocationCode())
                            .quantity(0)
                            .build());
            inventory.setQuantity(inventory.getQuantity() + itemRequest.getQuantity());
            inventoryRepository.save(inventory);

            orderItems.add(InboundOrderItem.builder()
                    .orderId(order.getId())
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .locationCode(itemRequest.getLocationCode())
                    .build());
            itemResponses.add(InboundOrderItemResponse.builder()
                    .productId(itemRequest.getProductId())
                    .productName(products.get(itemRequest.getProductId()).getName())
                    .quantity(itemRequest.getQuantity())
                    .locationCode(itemRequest.getLocationCode())
                    .build());
        }
        inboundOrderItemRepository.saveAll(orderItems);

        log.info("创建入库单成功: orderNo={}, itemCount={}", order.getOrderNo(), orderItems.size());
        return InboundOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private Map<Long, Product> validateProducts(List<InboundItemRequest> items) {
        Map<Long, Product> products = new HashMap<>();
        for (InboundItemRequest item : items) {
            products.computeIfAbsent(item.getProductId(), id -> productRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(404, "商品不存在: " + id)));
        }
        return products;
    }

    private void validateLocations(List<InboundItemRequest> items) {
        for (InboundItemRequest item : items) {
            if (!locationRepository.existsByCode(item.getLocationCode())) {
                throw new BusinessException(404, "库位不存在: " + item.getLocationCode());
            }
        }
    }

    private String generateOrderNo() {
        String prefix = "IN-" + LocalDate.now().format(ORDER_DATE_FORMATTER) + "-";
        int nextSequence = inboundOrderRepository
                .findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
                .map(InboundOrder::getOrderNo)
                .map(orderNo -> Integer.parseInt(orderNo.substring(prefix.length())) + 1)
                .orElse(1);
        if (nextSequence > 999) {
            throw new BusinessException("当日入库单数量已达上限");
        }
        return prefix + String.format("%03d", nextSequence);
    }

    /**
     * 库存查询 — 候选人实现
     */
    public PageResult<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                         int page, int pageSize) {
        // TODO: 候选人实现
        throw new UnsupportedOperationException("请实现库存查询功能（任务2）");
    }
}
