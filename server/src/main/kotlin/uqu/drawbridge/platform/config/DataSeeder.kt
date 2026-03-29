package uqu.drawbridge.platform.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Configuration
class DataSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val productRepository: ProductRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val orderRepository: OrderRepository,
    private val orderGroupRepository: OrderGroupRepository,
    private val orderItemRepository: OrderItemRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val productRatingRepository: ProductRatingRepository,
) {

    @Bean
    fun seedData(): CommandLineRunner {
        return CommandLineRunner {
            if (categoryRepository.count() == 0L) {
                println("SEEDING DEFAULT CATEGORIES...")
                val defaultCategoryNames = listOf(
                    "Food & Beverages",
                    "Supplies",
                    "Electronics",
                    "Equipment",
                    "Packaging",
                    "Cleaning & Hygiene",
                    "Baking & Ingredients",
                    "Dairy & Refrigerated",
                    "Frozen Foods",
                    "Snacks & Confectionery",
                    "Beverage Accessories",
                    "Office & Admin"
                )
                defaultCategoryNames.forEach { categoryName ->
                    categoryRepository.save(Category(name = categoryName))
                }
            }

            if (userRepository.count() > 0) return@CommandLineRunner

            println("SEEDING DATA...")

            // 1. Users
            val wholesaler = User(
                email = "wholesaler@test.com",
                passwordHash = passwordEncoder.encode("password") ?: "hash",
                role = UserRole.WHOLESALER,
                phoneNumber = "0500000001",
                businessName = "Jeddah Roasters",
                avatar = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRwLvHUwaBjKa2Cqd1JCjEAVvMfO6abwzpTRw&s",
                commercialRegistrationNumber = "CR123456",
                verificationStatus = true
            )
            wholesaler.representative = Representative(
                name = "Jeddah Roasters Rep",
                jobTitle = "Manager",
                phoneNumber = "0501111111",
                email = "rep@wholesaler.com"
            )
            
            wholesaler.addresses.add(Address(
                street = "Jeddah Industrial City",
                city = "Jeddah",
                state = "Makkah",
                zipCode = "21411",
                country = "Saudi Arabia"
            ))
            
            userRepository.save(wholesaler)

            val retailer = User(
                email = "retailer@test.com",
                passwordHash = passwordEncoder.encode("password") ?: "hash",
                role = UserRole.RETAILER,
                phoneNumber = "0500000002",
                businessName = "My Coffee House",
                avatar = "https://static.vecteezy.com/system/resources/thumbnails/004/854/407/small/coffee-house-logo-with-cup-of-coffee-and-roof-icon-symbol-free-vector.jpg",
                commercialRegistrationNumber = "CR789012",
                verificationStatus = true
            )
            retailer.representative = Representative(
                name = "Coffee House Owner",
                jobTitle = "Owner",
                phoneNumber = "0502222222",
                email = "owner@coffeehouse.com"
            )
            
            retailer.addresses.add(Address(
                street = "Riyadh Blvd",
                city = "Riyadh",
                state = "Riyadh",
                zipCode = "11564",
                country = "Saudi Arabia"
            ))

            val savedRetailer = userRepository.save(retailer)

            // Seed Payment Methods
            paymentMethodRepository.save(PaymentMethod(
                ownerId = savedRetailer.id!!,
                type = uqu.drawbridge.platform.PaymentMethodType.CREDIT_CARD,
                maskedDetails = "Visa **** 4444 (Exp: 12/28)",
                isDefault = true
            ))


            // 2. Categories
            val categoriesByName = categoryRepository.findAll().associateBy { it.name }
            val catFood = categoriesByName["Food & Beverages"] ?: categoryRepository.save(Category(name = "Food & Beverages"))
            val catSupplies = categoriesByName["Supplies"] ?: categoryRepository.save(Category(name = "Supplies"))
            val catElectronics = categoriesByName["Electronics"] ?: categoryRepository.save(Category(name = "Electronics"))

            // 3. Products
            val p1 = Product(
                name = "Premium Coffee Beans",
                description = "High-quality Arabica coffee beans, freshly roasted",
                price = BigDecimal("89.99"),
                stockQuantity = 150,
                wholesaler = wholesaler,
                categoryId = catFood.id!!,
                published = true,
                averageRating = BigDecimal("4.8"),
                ratingCount = 124
            )
            p1.images.add(ProductImage(url = "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=800&q=80", altText = "Premium Coffee Beans"))
            p1.images.add(ProductImage(url = "https://images.unsplash.com/photo-1580915411954-282cb1b0d780?w=800&q=80", altText = "Coffee roasting process"))
            val savedP1 = productRepository.save(p1)

            val p2 = Product(
                name = "Colombian Single Origin",
                description = "Smooth, medium-bodied coffee with notes of caramel and citrus",
                price = BigDecimal("145.00"),
                stockQuantity = 80,
                wholesaler = wholesaler,
                categoryId = catFood.id!!,
                published = true,
                averageRating = BigDecimal("4.6"),
                ratingCount = 56
            )
            p2.images.add(ProductImage(url = "https://images.unsplash.com/photo-1511537190424-bbbab87ac5eb?w=800&q=80", altText = "Colombian Single Origin Beans"))
            val savedP2 = productRepository.save(p2)

            val p3 = Product(
                name = "Espresso Roast Blend 5kg",
                description = "Our signature dark roast blend optimized for espresso extraction",
                price = BigDecimal("289.00"),
                stockQuantity = 50,
                wholesaler = wholesaler,
                categoryId = catFood.id!!,
                published = true,
                averageRating = BigDecimal("4.9"),
                ratingCount = 89
            )
            p3.images.add(ProductImage(url = "https://images.unsplash.com/photo-1552611052-33e04de081de?w=800&q=80", altText = "Espresso Roast Bulk Bag"))
            val savedP3 = productRepository.save(p3)

            // Seed some ratings
            productRatingRepository.save(ProductRating(
                productId = savedP1.id!!,
                userId = savedRetailer.id!!,
                rating = 5,
                review = "Excellent coffee beans! My customers love it."
            ))

            productRatingRepository.save(ProductRating(
                productId = savedP2.id!!,
                userId = savedRetailer.id!!,
                rating = 4,
                review = "Great quality, but the packaging could be better."
            ))

            // 4. Inventory (for Retailer)
            inventoryItemRepository.save(InventoryItem(
                productId = savedP1.id!!,
                retailerId = savedRetailer.id!!,
                currentQuantity = 5,
                lastUpdated = LocalDateTime.now(),
                autoOrderConfig = AutoOrderConfig(
                    enabled = true,
                    minThreshold = 10,
                    reorderQuantity = 20
            )))

            inventoryItemRepository.save(InventoryItem(
                productId = savedP2.id!!,
                retailerId = savedRetailer.id!!,
                currentQuantity = 8,
                lastUpdated = LocalDateTime.now(),
                autoOrderConfig = AutoOrderConfig(
                    enabled = true,
                    minThreshold = 5,
                    reorderQuantity = 10
                )
            ))

            // 5. Orders
            val orderGroup = orderGroupRepository.save(OrderGroup(
                retailerId = savedRetailer.id!!,
                groupTotal = BigDecimal("456.78"),
                paymentStatus = PaymentStatus.COMPLETED
            ))

            val order = Order(
                retailerId = savedRetailer.id!!,
                wholesalerId = wholesaler.id!!,
                status = OrderStatus.DELIVERED,
                subtotal = BigDecimal("456.78")
            )
            
            // Link order items
            val orderItem = OrderItem(
                productId = savedP1.id!!,
                quantity = 5,
                unitPrice = savedP1.price
            )
            
            order.orderItems.add(orderItem)
            orderGroup.orders.add(order)
            
            orderGroupRepository.save(orderGroup)

            println("DATA SEEDING COMPLETED.")
        }
    }
}
