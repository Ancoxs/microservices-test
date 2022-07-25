package com.testproject.inventoryservice;

import com.testproject.inventoryservice.model.Inventory;
import com.testproject.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableEurekaClient
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(InventoryRepository inventoryRepository){
        return args -> {
            if (inventoryRepository.findAll().isEmpty()){
                Inventory inventory = new Inventory();
                inventory.setSkuCode("sku123");
                inventory.setQuantity(23);

                Inventory inventory1 = new Inventory();
                inventory1.setSkuCode("sku1234");
                inventory1.setQuantity(0);

                inventoryRepository.save(inventory);
                inventoryRepository.save(inventory1);
            }
        };
    }
}
