package com.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 入库单明细请求。
 *
 * <p>该类型需被 Controller 和 Service 同时使用，因此单独声明为 public 类。</p>
 */
@Data
public class InboundItemRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "库位编码不能为空")
    private String locationCode;
}
