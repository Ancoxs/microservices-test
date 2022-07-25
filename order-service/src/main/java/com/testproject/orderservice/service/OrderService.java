package com.testproject.orderservice.service;

import com.testproject.orderservice.dto.InventoryResponse;
import com.testproject.orderservice.dto.OrderLineItemsDto;
import com.testproject.orderservice.dto.OrderRequest;
import com.testproject.orderservice.model.Order;
import com.testproject.orderservice.model.OrderLineItems;
import com.testproject.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private  final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItems(orderLineItems);

        if(allOrderedProductsInStock(orderLineItems)) {
            orderRepository.save(order);
            return "Order placed successfully";
        }else throw new IllegalArgumentException("Product is not in stock. Please try again later.");
    }

    private boolean allOrderedProductsInStock(List<OrderLineItems> orderLineItems){
        List<String> ordersSku = orderLineItems.stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("Inventory Service lookup");
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())){
            //        Call Inventory Service synchronously and place order if product is in stock
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode", ordersSku).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponses)
                    .allMatch(InventoryResponse::isInStock);

            return allProductsInStock && inventoryResponses.length >0;
        }finally {
            inventoryServiceLookup.end();
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
