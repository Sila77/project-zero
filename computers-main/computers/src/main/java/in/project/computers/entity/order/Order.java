package in.project.computers.entity.order;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
@Data
@Builder
public class Order {
    @Id
    private String id;


    private String userId;
    private String userAddress;
    private String phoneNumber;
    private String email;


    private List<OrderLineItem> lineItems;
    private BigDecimal totalAmount;
    private String currency;


    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;


    private PaymentDetails paymentDetails;


    private Instant createdAt;
    private Instant updatedAt;
}