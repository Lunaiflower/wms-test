package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundItemRequest;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InboundOrderRepository inboundOrderRepository;
    @Mock
    private InboundOrderItemRepository inboundOrderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void createInboundOrder_shouldCreateOrderAndIncreaseExistingInventory() {
        InboundOrderCreateRequest request = createRequest(1L, 20, "WH-A-01-01");
        Product product = Product.builder().id(1L).name("蓝牙耳机").sku("SKU-001").build();
        Inventory inventory = Inventory.builder()
                .id(10L)
                .productId(1L)
                .locationCode("WH-A-01-01")
                .quantity(80)
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.existsByCode("WH-A-01-01")).thenReturn(true);
        when(inboundOrderRepository.findTopByOrderNoStartingWithOrderByOrderNoDesc(any()))
                .thenReturn(Optional.empty());
        when(inboundOrderRepository.save(any(InboundOrder.class))).thenAnswer(invocation -> {
            InboundOrder order = invocation.getArgument(0);
            order.setId(100L);
            order.setCreatedAt(LocalDateTime.of(2026, 8, 19, 10, 0));
            return order;
        });
        when(inventoryRepository.findByProductIdAndLocationCode(1L, "WH-A-01-01"))
                .thenReturn(Optional.of(inventory));

        InboundOrderResponse response = inventoryService.createInboundOrder(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getOrderNo()).matches("IN-\\d{8}-001");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductName()).isEqualTo("蓝牙耳机");
                    assertThat(item.getQuantity()).isEqualTo(20);
                    assertThat(item.getLocationCode()).isEqualTo("WH-A-01-01");
                });
        assertThat(inventory.getQuantity()).isEqualTo(100);

        ArgumentCaptor<InboundOrder> orderCaptor = ArgumentCaptor.forClass(InboundOrder.class);
        verify(inboundOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getSupplierName()).isEqualTo("供应商A");
        verify(inventoryRepository).save(inventory);
        verify(inboundOrderItemRepository).saveAll(any());
    }

    @Test
    void createInboundOrder_shouldRejectMissingProductBeforeAnyWrite() {
        InboundOrderCreateRequest request = createRequest(999L, 20, "WH-A-01-01");
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.createInboundOrder(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo(404);
                    assertThat(exception.getMessage()).isEqualTo("商品不存在: 999");
                });

        verify(productRepository).findById(999L);
        verify(locationRepository, never()).existsByCode(any());
        verifyNoInteractions(inventoryRepository, inboundOrderRepository, inboundOrderItemRepository);
    }

    private InboundOrderCreateRequest createRequest(Long productId, int quantity, String locationCode) {
        InboundItemRequest item = new InboundItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setLocationCode(locationCode);

        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplierName("供应商A");
        request.setItems(List.of(item));
        return request;
    }
}
