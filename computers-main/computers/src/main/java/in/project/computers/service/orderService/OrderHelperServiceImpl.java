package in.project.computers.service.orderService;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.dto.order.CreateOrderRequest;
import in.project.computers.dto.order.OrderResponse;
import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.ComponentRepo.InventoryRepository;
import in.project.computers.repository.generalRepo.ComputerBuildRepository;
import in.project.computers.repository.generalRepo.OrderRepository;
import in.project.computers.service.PaypalService.PaypalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class OrderHelperServiceImpl implements OrderHelperService {

    // --- NO CHANGE ---
    // The dependencies and class properties have not changed from your original file.
    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final ComputerBuildRepository buildRepository;
    private final OrderRepository orderRepository;
    private final PaypalService paypalService;
    private final APIContext apiContext;

    @Value("${app.currency:THB}")
    private String currency;
    // --- END NO CHANGE ---


    // --- METHOD: createAndValidateBaseOrder (HEAVILY REFACTORED) ---
    // This method was changed significantly to fix the main problem.
    //
    // ORIGINAL LOGIC:
    // This method used to take a pre-processed `aggregatedItems` map. This caused the problem because
    // it lost the information about which items were in a "build" and which were separate.
    //
    //      // ORIGINAL SIGNATURE:
    //      public Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser, Map<String, Integer> aggregatedItems) {
    //          // It would then loop through the flat 'aggregatedItems' map
    //          for (Map.Entry<String, Integer> entry : aggregatedItems.entrySet()) {
    //              // ... logic to create OrderItem
    //          }
    //          // And build an Order with a flat list of components
    //          Order order = Order.builder().orderItems(orderItems).build();
    //      }
    //
    // NEW LOGIC:
    // 1. The method now takes the raw `CreateOrderRequest` and handles everything inside.
    // 2. It processes `buildItems` and `componentItems` separately to create structured `OrderLineItem`s.
    // 3. This preserves the important information for the seller.
    @Override
    public Order createAndValidateBaseOrder(CreateOrderRequest request, UserEntity currentUser) {
        if ((request.getBuildItems() == null || request.getBuildItems().isEmpty()) &&
                (request.getComponentItems() == null || request.getComponentItems().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be empty.");
        }

        List<OrderLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<String, Component> allNeededComponents = fetchAllRequiredComponents(request);
        Map<String, Inventory> allNeededInventories = fetchAllRequiredInventories(allNeededComponents.keySet());

        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            List<ComputerBuild> builds = buildRepository.findAllById(request.getBuildItems().keySet());
            for (ComputerBuild build : builds) {
                int quantityOrdered = request.getBuildItems().get(build.getId());
                OrderLineItem buildLineItem = createBuildLineItem(build, quantityOrdered, allNeededInventories);
                lineItems.add(buildLineItem);
                totalAmount = totalAmount.add(buildLineItem.getUnitPrice().multiply(BigDecimal.valueOf(quantityOrdered)));
            }
        }

        if (request.getComponentItems() != null && !request.getComponentItems().isEmpty()) {
            for (Map.Entry<String, Integer> entry : request.getComponentItems().entrySet()) {
                String componentId = entry.getKey();
                int quantity = entry.getValue();
                OrderLineItem componentLineItem = createComponentLineItem(componentId, quantity, allNeededComponents, allNeededInventories);
                lineItems.add(componentLineItem);
                totalAmount = totalAmount.add(componentLineItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            }
        }

        validateOverallStock(request, allNeededInventories);

        Order order = Order.builder()
                .userId(currentUser.getId())
                .userAddress(request.getUserAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(currentUser.getEmail())
                .lineItems(lineItems) // Changed from .orderItems()
                .totalAmount(totalAmount)
                .currency(this.currency)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        log.info("Structured base order created for user: {}. Total: {} {}. Line Items: {}",
                currentUser.getEmail(), totalAmount, this.currency, lineItems.size());
        return order;
    }


    // --- NEW HELPER METHODS ---
    // These methods did not exist in your original file. They were created to support
    // the new, structured way of handling orders.
    private OrderLineItem createBuildLineItem(ComputerBuild build, int quantity, Map<String, Inventory> inventoryMap) {
        List<OrderItemSnapshot> snapshots = new ArrayList<>();

        forEachComponentInBuild(build, (component, qty) -> {
            Inventory inventory = inventoryMap.get(component.getId());
            if (inventory == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Inventory missing for " + component.getName());
            snapshots.add(new OrderItemSnapshot(component.getId(), component.getName(), component.getMpn(), qty, inventory.getPrice()));
        });

        BigDecimal buildPrice = snapshots.stream()
                .map(s -> s.getPriceAtTimeOfOrder().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderLineItem.builder()
                .itemType(LineItemType.BUILD).name(build.getBuildName()).quantity(quantity)
                .unitPrice(buildPrice).buildId(build.getId()).containedItems(snapshots)
                .build();
    }

    private OrderLineItem createComponentLineItem(String componentId, int quantity, Map<String, Component> componentMap, Map<String, Inventory> inventoryMap) {
        Component component = componentMap.get(componentId);
        Inventory inventory = inventoryMap.get(componentId);
        if (component == null || inventory == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Standalone component with ID " + componentId + " could not be found.");
        }

        return OrderLineItem.builder()
                .itemType(LineItemType.COMPONENT).name(component.getName()).quantity(quantity)
                .unitPrice(inventory.getPrice()).componentId(component.getId()).mpn(component.getMpn())
                .build();
    }
    // --- END NEW HELPER METHODS ---


    // --- METHOD: decrementStockForOrder & incrementStockForOrder (REFACTORED) ---
    // ORIGINAL LOGIC:
    // Looped through the simple `order.getOrderItems()` list.
    //
    //      // ORIGINAL CODE:
    //      public void decrementStockForOrder(Order order) {
    //          for (OrderItem item : order.getOrderItems()) {
    //              // ... logic to update stock based on item.getQuantity() ...
    //          }
    //      }
    //
    // NEW LOGIC:
    // Now loops through the structured `order.getLineItems()` list. It checks if an item is a
    // BUILD or a COMPONENT and correctly calculates stock changes for all parts inside a build.
    @Override
    public void decrementStockForOrder(Order order) {
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                updateStock(lineItem.getComponentId(), -lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToRemove = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), -totalQuantityToRemove);
                }
            }
        }
        log.info("Stock successfully decremented for order ID: {}", order.getId());
    }

    @Override
    public void incrementStockForOrder(Order order) {
        for (OrderLineItem lineItem : order.getLineItems()) {
            if (lineItem.getItemType() == LineItemType.COMPONENT) {
                updateStock(lineItem.getComponentId(), lineItem.getQuantity());
            } else if (lineItem.getItemType() == LineItemType.BUILD) {
                for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                    int totalQuantityToAdd = part.getQuantity() * lineItem.getQuantity();
                    updateStock(part.getComponentId(), totalQuantityToAdd);
                }
            }
        }
        log.info("Stock successfully incremented for order ID: {}", order.getId());
    }

    // --- This private helper `updateStock` is new, but it contains logic similar to what was inside the original stock methods.
    private void updateStock(String componentId, int quantityChange) {
        Inventory inventory = inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new IllegalStateException("Data Inconsistency: Inventory not found for component ID " + componentId));
        int newQuantity = inventory.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock for component ID " + componentId + " was depleted.");
        }
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        componentRepository.findById(componentId).ifPresent(component -> {
            boolean shouldBeActive = newQuantity > 0;
            if (component.isActive() != shouldBeActive) {
                component.setActive(shouldBeActive);
                componentRepository.save(component);
            }
        });
    }


    // --- NEW HELPER METHODS ---
    // These methods (`validateOverallStock`, `fetchAllRequiredComponents`, `forEachComponentInBuild`) are new or heavily refactored
    // from your original `unpackAndAggregateItems` method, but now work correctly with the new structure.
    private void validateOverallStock(CreateOrderRequest request, Map<String, Inventory> inventoryMap) {
        Map<String, Integer> requiredStock = new HashMap<>();
        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            List<ComputerBuild> builds = buildRepository.findAllById(request.getBuildItems().keySet());
            for (ComputerBuild build : builds) {
                int buildQty = request.getBuildItems().get(build.getId());
                forEachComponentInBuild(build, (component, qty) ->
                        requiredStock.merge(component.getId(), qty * buildQty, Integer::sum)
                );
            }
        }
        if (request.getComponentItems() != null) {
            request.getComponentItems().forEach((id, qty) -> requiredStock.merge(id, qty, Integer::sum));
        }
        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            Inventory inventory = inventoryMap.get(componentId);
            if (inventory == null || inventory.getQuantity() < required) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for component ID " + componentId);
            }
        }
    }

    private Map<String, Component> fetchAllRequiredComponents(CreateOrderRequest request) {
        List<String> componentIds = new ArrayList<>();
        if (request.getComponentItems() != null) {
            componentIds.addAll(request.getComponentItems().keySet());
        }
        if (request.getBuildItems() != null && !request.getBuildItems().isEmpty()) {
            buildRepository.findAllById(request.getBuildItems().keySet()).forEach(build ->
                    forEachComponentInBuild(build, (component, qty) -> componentIds.add(component.getId()))
            );
        }
        return componentRepository.findAllById(componentIds).stream().collect(Collectors.toMap(Component::getId, Function.identity()));
    }

    private Map<String, Inventory> fetchAllRequiredInventories(Iterable<String> componentIds) {
        List<String> idList = new ArrayList<>();
        componentIds.forEach(idList::add);
        return inventoryRepository.findAllByComponentIdIn(idList).stream()
                .collect(Collectors.toMap(Inventory::getComponentId, Function.identity()));
    }

    private void forEachComponentInBuild(ComputerBuild build, BiConsumer<Component, Integer> action) {
        Stream.of(build.getCpu(), build.getMotherboard(), build.getPsu(), build.getCaseDetail(), build.getCooler())
                .filter(Objects::nonNull)
                .forEach(component -> action.accept(component, 1));

        if (build.getRamKits() != null) {
            build.getRamKits().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getGpus() != null) {
            build.getGpus().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getStorageDrives() != null) {
            build.getStorageDrives().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
    }
    // --- END NEW HELPER METHODS ---


    // --- METHOD: processPaypalRefund & extractSaleIdFromPaypalPayment (REFACTORED) ---
    // ORIGINAL LOGIC:
    // The refund logic was originally inside the main `OrderServiceImpl` in the `approveRefund` method.
    // This was not good practice.
    //
    // NEW LOGIC:
    // The logic has been moved here, into the helper service, so the main service just needs to call it.
    // This is a much cleaner design. The core logic itself is very similar to what was in your original `approveRefund`.
    @Override
    public void processPaypalRefund(Order order, PaymentDetails paymentDetails) throws PayPalRESTException {
        if (paymentDetails.getTransactionId() == null || paymentDetails.getTransactionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Original PayPal Transaction ID not found for this order.");
        }
        Payment originalPayment = Payment.get(this.apiContext, paymentDetails.getTransactionId());
        String saleId = extractSaleIdFromPaypalPayment(originalPayment, order.getId());
        Refund refund = paypalService.refundPayment(saleId, null, order.getCurrency());
        if ("completed".equalsIgnoreCase(refund.getState()) || "pending".equalsIgnoreCase(refund.getState())) {
            paymentDetails.setProviderStatus("refunded: " + refund.getState());
            paymentDetails.setTransactionId(refund.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PayPal refund failed. State: " + refund.getState());
        }
    }

    @Override
    public String extractSaleIdFromPaypalPayment(Payment originalPaypalPayment, String orderIdForLog) {
        if (originalPaypalPayment == null || originalPaypalPayment.getTransactions() == null || originalPaypalPayment.getTransactions().isEmpty() ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources() == null ||
                originalPaypalPayment.getTransactions().getFirst().getRelatedResources().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID: Invalid PayPal payment structure.");
        }
        Sale sale = originalPaypalPayment.getTransactions().getFirst().getRelatedResources().getFirst().getSale();
        if (sale == null || sale.getId() == null || sale.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not derive Sale ID from PayPal payment's related resources.");
        }
        return sale.getId();
    }


    // --- NO CHANGE ---
    // This method has the same logic as your original file.
    @Override
    public Order findOrderForProcessing(String orderId, String userId, PaymentMethod expectedMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this order.");
        }
        if (order.getPaymentDetails() == null || order.getPaymentDetails().getPaymentMethod() != expectedMethod) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect payment method for this action.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is not pending payment.");
        }
        return order;
    }
    // --- END NO CHANGE ---


    // --- METHOD: entityToResponse (MINOR CHANGE) ---
    // ORIGINAL LOGIC:
    // Mapped the old `orderItems` field.
    //
    //      // ORIGINAL CODE:
    //      return OrderResponse.builder()
    //          ...
    //          .orderItems(order.getOrderItems())
    //          ...
    //          .build();
    //
    // NEW LOGIC:
    // Now maps the new, structured `lineItems` field.
    @Override
    public OrderResponse entityToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userAddress(order.getUserAddress())
                .phoneNumber(order.getPhoneNumber())
                .email(order.getEmail())
                .lineItems(order.getLineItems())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentDetails(order.getPaymentDetails())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}