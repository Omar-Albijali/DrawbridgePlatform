package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.ProductDiscount
import uqu.drawbridge.platform.repository.ProductDiscountRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class ProductDiscountService(
    private val productDiscountRepository: ProductDiscountRepository
) {

    /**
     * Create a new discount for a product.
     */
    @Transactional
    fun createDiscount(
        productId: String,
        name: String,
        discountPercentage: BigDecimal,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        description: String? = null,
        isActive: Boolean = true
    ): ProductDiscount {
        require(discountPercentage > BigDecimal.ZERO && discountPercentage <= BigDecimal(100)) {
            "Discount percentage must be between 0 and 100"
        }
        require(endDate.isAfter(startDate)) {
            "End date must be after start date"
        }

        val discount = ProductDiscount(
            productId = productId,
            name = name,
            description = description,
            discountPercentage = discountPercentage,
            startDate = startDate,
            endDate = endDate,
            isActive = isActive
        )

        return productDiscountRepository.save(discount)
    }

    /**
     * Update an existing discount.
     */
    @Transactional
    fun updateDiscount(
        discountId: String,
        name: String? = null,
        discountPercentage: BigDecimal? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        description: String? = null,
        isActive: Boolean? = null
    ): ProductDiscount? {
        val discount = productDiscountRepository.findById(discountId).orElse(null) ?: return null

        name?.let { discount.name = it }
        discountPercentage?.let {
            require(it > BigDecimal.ZERO && it <= BigDecimal(100)) {
                "Discount percentage must be between 0 and 100"
            }
            discount.discountPercentage = it
        }
        startDate?.let { discount.startDate = it }
        endDate?.let { discount.endDate = it }
        description?.let { discount.description = it }
        isActive?.let { discount.isActive = it }

        // Validate dates after update
        require(discount.endDate.isAfter(discount.startDate)) {
            "End date must be after start date"
        }

        return productDiscountRepository.save(discount)
    }

    /**
     * Get all discounts for a product.
     */
    fun getDiscountsByProductId(productId: String): List<ProductDiscount> {
        return productDiscountRepository.findByProductId(productId)
    }

    /**
     * Get currently active discounts for a product.
     */
    fun getActiveDiscounts(productId: String): List<ProductDiscount> {
        return productDiscountRepository.findActiveDiscountsByProductId(productId, LocalDateTime.now())
    }

    /**
     * Get the best (highest) active discount for a product.
     */
    fun getBestActiveDiscount(productId: String): ProductDiscount? {
        return getActiveDiscounts(productId).maxByOrNull { it.discountPercentage }
    }

    /**
     * Get the best active discount for each product in the provided collection.
     */
    fun getBestActiveDiscounts(productIds: Collection<String>): Map<String, ProductDiscount> {
        if (productIds.isEmpty()) {
            return emptyMap()
        }

        return productDiscountRepository.findActiveDiscountsByProductIds(productIds, LocalDateTime.now())
            .groupBy { it.productId }
            .mapValues { (_, discounts) -> discounts.maxByOrNull { it.discountPercentage }!! }
    }

    /**
     * Calculate the discounted price for a product.
     */
    fun calculateDiscountedPrice(originalPrice: BigDecimal, productId: String): BigDecimal {
        val bestDiscount = getBestActiveDiscount(productId) ?: return originalPrice

        val discountAmount = originalPrice.multiply(bestDiscount.discountPercentage)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

        return originalPrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Delete a discount.
     */
    @Transactional
    fun deleteDiscount(discountId: String): Boolean {
        val discount = productDiscountRepository.findById(discountId).orElse(null) ?: return false
        productDiscountRepository.delete(discount)
        return true
    }

    /**
     * Toggle discount active status.
     */
    @Transactional
    fun toggleDiscountStatus(discountId: String): ProductDiscount? {
        val discount = productDiscountRepository.findById(discountId).orElse(null) ?: return null
        discount.isActive = !discount.isActive
        return productDiscountRepository.save(discount)
    }
}
