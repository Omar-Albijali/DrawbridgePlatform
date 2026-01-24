package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.ProductRating
import uqu.drawbridge.platform.repository.ProductRatingRepository
import uqu.drawbridge.platform.repository.ProductRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class ProductRatingService(
    private val productRatingRepository: ProductRatingRepository,
    private val productRepository: ProductRepository
) {

    /**
     * Add or update a rating for a product by a user.
     * If the user has already rated the product, the rating is updated.
     * Automatically recalculates the product's average rating.
     */
    @Transactional
    fun addOrUpdateRating(productId: String, userId: String, rating: Int, review: String? = null): ProductRating {
        require(rating in 1..5) { "Rating must be between 1 and 5" }

        val existingRating = productRatingRepository.findByProductIdAndUserId(productId, userId)

        val savedRating = if (existingRating != null) {
            existingRating.rating = rating
            existingRating.review = review
            existingRating.updatedAt = LocalDateTime.now()
            productRatingRepository.save(existingRating)
        } else {
            val newRating = ProductRating(
                productId = productId,
                userId = userId,
                rating = rating,
                review = review
            )
            productRatingRepository.save(newRating)
        }

        // Update product's average rating
        updateProductRatingSummary(productId)

        return savedRating
    }

    /**
     * Get all ratings for a product.
     */
    fun getRatingsByProductId(productId: String): List<ProductRating> {
        return productRatingRepository.findByProductId(productId)
    }

    /**
     * Get all ratings by a user.
     */
    fun getRatingsByUserId(userId: String): List<ProductRating> {
        return productRatingRepository.findByUserId(userId)
    }

    /**
     * Get a specific rating by product and user.
     */
    fun getRating(productId: String, userId: String): ProductRating? {
        return productRatingRepository.findByProductIdAndUserId(productId, userId)
    }

    /**
     * Delete a rating and recalculate the product's average.
     */
    @Transactional
    fun deleteRating(ratingId: String): Boolean {
        val rating = productRatingRepository.findById(ratingId).orElse(null) ?: return false
        val productId = rating.productId

        productRatingRepository.delete(rating)
        updateProductRatingSummary(productId)

        return true
    }

    /**
     * Recalculate and update the product's average rating and rating count.
     */
    private fun updateProductRatingSummary(productId: String) {
        val product = productRepository.findById(productId).orElse(null) ?: return

        val count = productRatingRepository.countByProductId(productId)
        val average = productRatingRepository.getAverageRatingByProductId(productId)
            ?: BigDecimal.ZERO

        product.ratingCount = count
        product.averageRating = average.setScale(2, RoundingMode.HALF_UP)

        productRepository.save(product)
    }
}
