package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.CartItemRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.ShoppingCartRepository
import uqu.drawbridge.platform.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CartService(
    private val shoppingCartRepository: ShoppingCartRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val orderService: OrderService
) {

    // ==================== CART OPERATIONS ====================

    fun getOrCreateCart(retailerId: String): ShoppingCart {
        return shoppingCartRepository.findByRetailerId(retailerId)
            .orElseGet {
                shoppingCartRepository.save(
                    ShoppingCart(
                        retailerId = retailerId,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
    }

    fun getCartByRetailerId(retailerId: String): ShoppingCart? {
        return shoppingCartRepository.findByRetailerId(retailerId).orElse(null)
    }

    fun getCartItems(retailerId: String): List<CartItem> {
        val cart = getCartByRetailerId(retailerId) ?: return emptyList()
        return cartItemRepository.findByCartId(cart.id!!)
    }

    fun getCartItemsGroupedByWholesaler(retailerId: String): Map<String, List<CartItem>> {
        val cart = getCartByRetailerId(retailerId) ?: return emptyMap()
        val items = cartItemRepository.findByCartId(cart.id!!)
        return items.groupBy { it.wholesalerId }
    }

    @Transactional
    fun addToCart(retailerId: String, productId: String, quantity: Int): CartItem? {
        val product = productRepository.findById(productId).orElse(null) ?: return null
        
        val cart = getOrCreateCart(retailerId)
        
        // Check if item already exists in cart
        val existingItem = cartItemRepository.findByCartIdAndProductId(cart.id!!, productId)
        
        return if (existingItem != null) {
            // Update quantity
            existingItem.quantity += quantity
            cart.updatedAt = LocalDateTime.now()
            shoppingCartRepository.save(cart)
            existingItem
        } else {
            // Add new item
            val cartItem = CartItem(
                productId = productId,
                wholesalerId = product.wholesaler.id!!,
                quantity = quantity,
                addedAt = LocalDateTime.now()
            )
            cart.items.add(cartItem)
            cart.updatedAt = LocalDateTime.now()
            shoppingCartRepository.save(cart)
            
            // Return the saved item from the cart list (latest one) or assuming it's persisted
            // For safety, we can return the item instance we just added
            cartItem
        }
    }

    @Transactional
    fun updateCartItemQuantity(retailerId: String, productId: String, quantity: Int): CartItem? {
        val cart = getCartByRetailerId(retailerId) ?: return null
        val item = cartItemRepository.findByCartIdAndProductId(cart.id!!, productId) ?: return null
        
        if (quantity <= 0) {
            removeFromCart(retailerId, productId)
            return null
        }
        
        item.quantity = quantity
        cart.updatedAt = LocalDateTime.now()
        shoppingCartRepository.save(cart)
        return item
    }

    @Transactional
    fun removeFromCart(retailerId: String, productId: String): Boolean {
        val cart = getCartByRetailerId(retailerId) ?: return false
        
        // Use removeIf on the collection to ensure orphanRemoval triggers properly
        val removed = cart.items.removeIf { it.productId == productId }
        
        if (removed) {
            cart.updatedAt = LocalDateTime.now()
            shoppingCartRepository.save(cart)
            return true
        }
        return false
    }

    @Transactional
    fun clearCart(retailerId: String): Boolean {
        val cart = getCartByRetailerId(retailerId) ?: return false
        cart.items.clear()
        cart.updatedAt = LocalDateTime.now()
        shoppingCartRepository.save(cart)
        return true
    }

    fun getCartItemCount(retailerId: String): Int {
        val cart = getCartByRetailerId(retailerId) ?: return 0
        return cartItemRepository.findByCartId(cart.id!!).sumOf { it.quantity }
    }

    fun getCartTotal(retailerId: String): BigDecimal {
        val cart = getCartByRetailerId(retailerId) ?: return BigDecimal.ZERO
        val items = cartItemRepository.findByCartId(cart.id!!)
        
        return items.fold(BigDecimal.ZERO) { total, item ->
            val product = productRepository.findById(item.productId).orElse(null)
            if (product != null) {
                total + (product.price * BigDecimal(item.quantity))
            } else {
                total
            }
        }
    }

    // ==================== CHECKOUT OPERATIONS ====================

    /**
     * Checkout the cart and create an OrderGroup with individual Orders per wholesaler.
     * Returns the created OrderGroup, or null if the cart is empty.
     */
    @Transactional
    fun checkout(retailerId: String): OrderGroup? {
        val cart = getCartByRetailerId(retailerId) ?: return null
        val items = cartItemRepository.findByCartId(cart.id!!)
        
        if (items.isEmpty()) return null
        
        // Group items by wholesaler
        val itemsByWholesaler = items.groupBy { it.wholesalerId }
        
        // Calculate group total
        var groupTotal = BigDecimal.ZERO
        
        // Create OrderGroup
        val orderGroup = orderService.createOrderGroup(
            OrderGroup(
                retailerId = retailerId,
                groupTotal = BigDecimal.ZERO, // Will be updated
                paymentStatus = PaymentStatus.PENDING
            )
        )
        
        // Create an Order for each wholesaler
        itemsByWholesaler.forEach { (wholesalerId, wholesalerItems) ->
            // Calculate subtotal for this wholesaler's order
            var subtotal = BigDecimal.ZERO
            
            val orderItems = wholesalerItems.mapNotNull { cartItem ->
                val product = productRepository.findById(cartItem.productId).orElse(null)
                if (product != null) {
                    val itemTotal = product.price * BigDecimal(cartItem.quantity)
                    subtotal += itemTotal
                    
                    OrderItem(
                        productId = cartItem.productId,
                        quantity = cartItem.quantity,
                        unitPrice = product.price
                    )
                } else null
            }
            
            groupTotal += subtotal
            
            // Create the order with items
            val order = Order(
                retailerId = retailerId,
                wholesalerId = wholesalerId,
                subtotal = subtotal,
                status = OrderStatus.PENDING
            )
            
            orderService.createOrderWithItems(order, orderItems)
            orderGroup.orders.add(order)
        }
        
        // Update group total
        orderGroup.groupTotal = groupTotal
        orderService.createOrderGroup(orderGroup)
        
        // Clear the cart after successful checkout
        clearCart(retailerId)
        
        return orderGroup
    }

    // ==================== DTO MAPPING ====================

    fun CartItem.toDTO() = CartItemDTO(
        id = this.id,
        productId = this.productId,
        wholesalerId = this.wholesalerId,
        quantity = this.quantity,
        addedAt = this.addedAt.toString()
    )

    fun ShoppingCart.toDTO() = ShoppingCartDTO(
        id = this.id ?: "",
        retailerId = this.retailerId,
        updatedAt = this.updatedAt.toString(),
        items = this.items.map { it.toDTO() }.toTypedArray()
    )

    // ==================== DTO-RETURNING METHODS ====================

    fun getOrCreateCartDTO(retailerId: String): ShoppingCartDTO = getOrCreateCart(retailerId).toDTO()

    fun getCartItemsDTO(retailerId: String): List<CartItemDTO> = getCartItems(retailerId).map { it.toDTO() }

    fun addToCartDTO(retailerId: String, productId: String, quantity: Int): CartItemDTO? = 
        addToCart(retailerId, productId, quantity)?.toDTO()

    fun updateCartItemQuantityDTO(retailerId: String, productId: String, quantity: Int): CartItemDTO? =
        updateCartItemQuantity(retailerId, productId, quantity)?.toDTO()
}
