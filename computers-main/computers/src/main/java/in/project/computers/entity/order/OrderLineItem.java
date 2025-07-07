package in.project.computers.entity.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItem {

    private LineItemType itemType;

    private String name;

    private int quantity;

    private BigDecimal unitPrice;

    private String componentId;

    private String mpn;

    private String buildId;

    private List<OrderItemSnapshot> containedItems;
}