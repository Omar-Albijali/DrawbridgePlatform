package uqu.drawbridge.platform.controller

import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import uqu.drawbridge.platform.ErrorResponse
import uqu.drawbridge.platform.ImageUploadResponse
import uqu.drawbridge.platform.ProductImageResponse
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.service.FileStorageService
import uqu.drawbridge.platform.service.ProductImageService

@RestController
@RequestMapping("/api")
class ImageController(
    private val fileStorageService: FileStorageService,
    private val productImageService: ProductImageService,
    private val userRepository: UserRepository
) {

    /**
     * Upload a profile image for a user
     */
    @PostMapping("/users/{userId}/profile-image")
    fun uploadUserProfileImage(
        @PathVariable userId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ImageUploadResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            java.util.NoSuchElementException("User not found")
        }

        // Delete old profile image if exists
        user.avatar?.let {
            val oldPath = it.removePrefix("/api/uploads/")
            fileStorageService.deleteFile(oldPath)
        }

        val relativePath = fileStorageService.storeFile(file, "profiles")
        val imageUrl = fileStorageService.getFileUrl(relativePath)

        user.avatar = imageUrl
        userRepository.save(user)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ImageUploadResponse(
                id = user.id,
                url = imageUrl,
                message = "Profile image uploaded successfully"
            )
        )
    }

    /**
     * Upload an image for a product
     */
    @PostMapping("/products/{productId}/images")
    fun uploadProductImage(
        @PathVariable productId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("altText", defaultValue = "") altText: String,
        @RequestParam("sortIndex", required = false) sortIndex: Int?
    ): ResponseEntity<ProductImageResponse> {
        val productImage = productImageService.uploadProductImage(productId, file, altText, sortIndex)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ProductImageResponse(
                id = productImage.id,
                url = productImage.url,
                altText = productImage.altText,
                sortIndex = productImage.sortIndex,
                productId = productImage.productId ?: ""
            )
        )
    }

    /**
     * Reorder product images. Accepts an ordered list of image IDs.
     * The first ID gets sortIndex 0 (main image), second gets 1, etc.
     */
    @PutMapping("/products/{productId}/images/reorder")
    fun reorderImages(
        @PathVariable productId: String,
        @RequestBody orderedImageIds: List<String>
    ): ResponseEntity<Void> {
        return if (productImageService.reorderImages(productId, orderedImageIds)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all images for a product (sorted by sortIndex)
     */
    @GetMapping("/products/{productId}/images")
    fun getProductImages(@PathVariable productId: String): ResponseEntity<List<ProductImageResponse>> {
        val images = productImageService.getProductImages(productId)
        return ResponseEntity.ok(images.sortedBy { it.sortIndex }.map {
            ProductImageResponse(
                id = it.id,
                url = it.url,
                altText = it.altText,
                sortIndex = it.sortIndex,
                productId = it.productId ?: ""
            )
        })
    }

    /**
     * Delete a product image
     */
    @DeleteMapping("/images/{imageId}")
    fun deleteImage(@PathVariable imageId: String): ResponseEntity<Void> {
        if (!productImageService.deleteProductImage(imageId)) {
             throw java.util.NoSuchElementException("Image not found")
        }
        return ResponseEntity.ok().build()
    }

    /**
     * Serve uploaded files
     */
    @GetMapping("/uploads/{subDir}/{filename:.+}")
    fun serveFile(
        @PathVariable subDir: String,
        @PathVariable filename: String
    ): ResponseEntity<Resource> {
        val resource = fileStorageService.loadFile("$subDir/$filename")
        val contentType = determineContentType(filename)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
            .body(resource)
    }

    private fun determineContentType(filename: String): String {
        val extension = filename.substringAfterLast(".").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }
}
