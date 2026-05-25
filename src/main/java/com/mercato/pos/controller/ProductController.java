package com.mercato.pos.controller;

import com.mercato.pos.dto.ProductDto;
import com.mercato.pos.service.ProductClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductClientService productClientService;

    public ProductController(ProductClientService productClientService) {
        this.productClientService = productClientService;
    }

    /**
     * Search products by name or barcode
     * GET /api/products/search?name=<name> or ?barcode=<barcode>
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String barcode) {

        if (name != null && !name.isEmpty()) {
            List<ProductDto> products = productClientService.searchByName(name);
            return ResponseEntity.ok(products);
        } else if (barcode != null && !barcode.isEmpty()) {
            ProductDto product = productClientService.searchByBarcode(barcode);
            return ResponseEntity.ok(product);
        } else {
            return ResponseEntity.badRequest().body("Debe proporcionar 'name' o 'barcode'");
        }
    }
}
