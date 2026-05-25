package com.mercato.pos.controller;

import com.mercato.pos.dto.CustomerDto;
import com.mercato.pos.service.CustomerClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerClientService customerClientService;

    public CustomerController(CustomerClientService customerClientService) {
        this.customerClientService = customerClientService;
    }

    /**
     * Search customers by name or document
     * GET /api/customers/search?name=<name> or ?document=<document>
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String document) {

        if (name != null && !name.isEmpty()) {
            List<CustomerDto> customers = customerClientService.searchByName(name);
            return ResponseEntity.ok(customers);
        } else if (document != null && !document.isEmpty()) {
            CustomerDto customer = customerClientService.searchByDocument(document);
            return ResponseEntity.ok(customer);
        } else {
            return ResponseEntity.badRequest().body("Debe proporcionar 'name' o 'document'");
        }
    }
}
