package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uqu.drawbridge.platform.model.ProductImage
import uqu.drawbridge.platform.repository.ProductImageRepository
import uqu.drawbridge.platform.repository.ProductRepository

@Service
class ProductImageService(
    private val productImageRepository: ProductImageRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService
) {

    @Transactional
    fun uploadProductImage(productId: String, file: MultipartFile, altText: String, sortIndex: Int? = null): ProductImage {
        val product = productRepository.findById(productId).orElseThrow {
            NoSuchElementException("Product not found: $productId")
        }

        val relativePath = fileStorageService.storeFile(file, "products")
        val imageUrl = fileStorageService.getFileUrl(relativePath)

        val nextIndex = sortIndex ?: (product.images.maxOfOrNull { it.sortIndex }?.plus(1) ?: 0)

        val productImage = ProductImage(
            url = imageUrl,
            altText = altText,
            sortIndex = nextIndex,
            product = product
        )

        // Add through parent collection so JPA sets the FK column
        product.images.add(productImage)
        productRepository.save(product)

        // Return the saved image (now has id and productId populated)
        return product.images.last()
    }

    @Transactional
    fun reorderImages(productId: String, orderedImageIds: List<String>): Boolean {
        val product = productRepository.findById(productId).orElse(null) ?: return false
        orderedImageIds.forEachIndexed { index, imageId ->
            product.images.find { it.id == imageId }?.sortIndex = index
        }
        productRepository.save(product)
        return true
    }

    fun getProductImages(productId: String): List<ProductImage> {
        return productImageRepository.findByProduct_Id(productId)
    }

    @Transactional
    fun deleteProductImage(imageId: String): Boolean {
        val image = productImageRepository.findById(imageId).orElse(null) ?: return false

        // Extract relative path from URL (remove "/uploads/" prefix)
        val relativePath = image.url.removePrefix("/api/uploads/")
        fileStorageService.deleteFile(relativePath)

        productImageRepository.delete(image)
        return true
    }
}
