package org.allen.paypal;

import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/paypal")
public class PayPalController {

    @Autowired
    private PaypalServerSdkClient client;

    @PostMapping("/create-order")
    public Map<String, String> createOrder(@RequestParam String amount) {
        try {
            OrdersController ordersController = client.getOrdersController();

            OrderRequest orderRequest = new OrderRequest.Builder(
                    CheckoutPaymentIntent.CAPTURE,
                    Arrays.asList(
                            new PurchaseUnitRequest.Builder(
                                    new AmountWithBreakdown.Builder("USD", amount)
                                            .build()
                            ).build()
                    )
            ).build();

            CreateOrderInput createOrderInput = new CreateOrderInput();
            createOrderInput.setBody(orderRequest);
            ApiResponse<Order> response = ordersController.createOrder(createOrderInput);
            Order order = response.getResult();

            // Get approval URL for frontend
            String approvalUrl = order.getLinks().stream()
                    .filter(link -> "approve".equals(link.getRel()))
                    .map(LinkDescription::getHref)
                    .findFirst()
                    .orElse("");

            return Map.of(
                    "orderID", order.getId(),
                    "approvalUrl", approvalUrl,
                    "status", order.getStatus().toString()
            );

        } catch (ApiException | IOException e) {
            return Map.of("error", "Failed to create order: " + e.getMessage());
        }
    }

    @PostMapping("/capture-order")
    public Map<String, String> captureOrder(@RequestParam String orderId) {
        try {
            OrdersController ordersController = client.getOrdersController();

            CaptureOrderInput captureOrderInput = new CaptureOrderInput();
            captureOrderInput.setId(orderId);
            ApiResponse<Order> response = ordersController.captureOrder(captureOrderInput);
            Order order = response.getResult();

            return Map.of(
                    "orderID", order.getId(),
                    "status", order.getStatus().toString()
            );

        } catch (ApiException | IOException e) {
            return Map.of("error", "Failed to capture order: " + e.getMessage());
        }
    }

    // Additional helper endpoint to get order details
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        try {
            OrdersController ordersController = client.getOrdersController();

            GetOrderInput getOrderInput = new GetOrderInput();
            getOrderInput.setId(orderId);
            ApiResponse<Order> response = ordersController.getOrder(getOrderInput);
            Order order = response.getResult();

            return Map.of(
                    "orderID", order.getId(),
                    "status", order.getStatus().toString(),
                    "intent", order.getIntent().toString(),
                    "createTime", order.getCreateTime() != null ? order.getCreateTime() : "N/A"
            );

        } catch (ApiException | IOException e) {
            return Map.of("error", "Failed to get order: " + e.getMessage());
        }
    }

    // Bonus: Create order with items
    @PostMapping("/create-order-with-items")
    public Map<String, String> createOrderWithItems(@RequestParam String amount) {
        try {
            OrdersController ordersController = client.getOrdersController();

            // Create items
            Item item = new Item.Builder(
                    "Sample Item",
                    new Money.Builder("USD", amount).build(),
                    "1")
                    .description("Sample product description")
                    .sku("SAMPLE-001")
                    .category(ItemCategory.PHYSICAL_GOODS)
                    .build();

            OrderRequest orderRequest = new OrderRequest.Builder(
                    CheckoutPaymentIntent.CAPTURE,
                    Arrays.asList(
                            new PurchaseUnitRequest.Builder(
                                    new AmountWithBreakdown.Builder("USD", amount).build())
                                    .items(Arrays.asList(item))
                                    .description("Sample purchase")
                                    .build()
                    )
            )
                    .applicationContext(
                            new OrderApplicationContext.Builder()
                                    .returnUrl("http://localhost:8080/paypal/success")
                                    .cancelUrl("http://localhost:8080/paypal/cancel")
                                    .build()
                    )
                    .build();

            CreateOrderInput createOrderInput = new CreateOrderInput();
            createOrderInput.setBody(orderRequest);
            ApiResponse<Order> response = ordersController.createOrder(createOrderInput);
            Order order = response.getResult();

            String approvalUrl = order.getLinks().stream()
                    .filter(link -> "approve".equals(link.getRel()))
                    .map(LinkDescription::getHref)
                    .findFirst()
                    .orElse("");

            return Map.of(
                    "orderID", order.getId(),
                    "approvalUrl", approvalUrl,
                    "status", order.getStatus().toString()
            );

        } catch (ApiException | IOException e) {
            return Map.of("error", "Failed to create order with items: " + e.getMessage());
        }
    }

    // Success/Cancel handlers
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String token, @RequestParam String PayerID) {
        // Auto-capture the payment
        try {
            OrdersController ordersController = client.getOrdersController();

            CaptureOrderInput captureOrderInput = new CaptureOrderInput();
            captureOrderInput.setPaypalRequestId(token);
            ApiResponse<Order> response = ordersController.captureOrder(captureOrderInput);
            Order order = response.getResult();

            return "Payment successful! Order ID: " + order.getId() +
                    ", Status: " + order.getStatus();
        } catch (ApiException | IOException e) {
            return "Payment approved but capture failed: " + e.getMessage();
        }
    }

    @GetMapping("/cancel")
    public String paymentCancel() {
        return "Payment was cancelled by user.";
    }
}

