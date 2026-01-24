package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uqu.drawbridge.platform.model.ProductImage
import uqu.drawbridge.platform.repository.ProductImageRepository

@Service
class ProductImageService(
    private val productImageRepository: ProductImageRepository,
    private val fileStorageService: FileStorageService
) {

    @Transactional
    fun uploadProductImage(productId: String, file: MultipartFile, altText: String): ProductImage {
        val relativePath = fileStorageService.storeFile(file, "products")
        val imageUrl = fileStorageService.getFileUrl(relativePath)
        
        val productImage = ProductImage(
            url = imageUrl,
            altText = altText,
            productId = productId
        )
        
        return productImageRepository.save(productImage)
    }

    fun getProductImages(productId: String): List<ProductImage> {
        return productImageRepository.findByProductId(productId)
    }

    @Transactional
    fun deleteProductImage(imageId: String): Boolean {
        val image = productImageRepository.findById(imageId).orElse(null) ?: return false
        
        // Extract relative path from URL (remove "/uploads/" prefix)
        val relativePath = image.url.removePrefix("/uploads/")
        fileStorageService.deleteFile(relativePath)
        
        productImageRepository.delete(image)
        return true
    }
}
