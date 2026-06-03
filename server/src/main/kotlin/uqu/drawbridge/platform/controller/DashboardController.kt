package uqu.drawbridge.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uqu.drawbridge.platform.MostOrderedProductDTO;
import uqu.drawbridge.platform.service.OrderService;


@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val orderService: OrderService
) {

    @GetMapping("/most-ordered")
    fun getMostOrderedProducts(@RequestParam retailerId: String):ResponseEntity<List<MostOrderedProductDTO>> {

        val result = orderService.getMostOrderedProducts(retailerId, minOrderCount = 3)
        return ResponseEntity.ok(result)

    }
}