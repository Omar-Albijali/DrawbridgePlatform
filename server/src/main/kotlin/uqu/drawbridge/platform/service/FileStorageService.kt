package uqu.drawbridge.platform.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService(
    @Value("\${file.upload-dir}") private val uploadDir: String
) {

    private lateinit var rootLocation: Path

    @PostConstruct
    fun init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize()
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw RuntimeException("Could not create upload directory!", e)
        }
    }

    /**
     * Stores a file in the specified subdirectory and returns the relative path
     */
    fun storeFile(file: MultipartFile, subDirectory: String): String {
        val originalFilename = file.originalFilename ?: "file"
        val extension = originalFilename.substringAfterLast(".", "")
        val uniqueFilename = "${UUID.randomUUID()}.$extension"
        
        val targetDir = rootLocation.resolve(subDirectory)
        Files.createDirectories(targetDir)
        
        val targetPath = targetDir.resolve(uniqueFilename)
        
        try {
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw RuntimeException("Failed to store file $uniqueFilename", e)
        }
        
        return "$subDirectory/$uniqueFilename"
    }

    /**
     * Loads a file as a Resource for serving
     */
    fun loadFile(filePath: String): Resource {
        val file = rootLocation.resolve(filePath).normalize()
        val resource = UrlResource(file.toUri())
        
        if (resource.exists() && resource.isReadable) {
            return resource
        }
        throw RuntimeException("Could not read file: $filePath")
    }

    /**
     * Deletes a file from storage
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = rootLocation.resolve(filePath).normalize()
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Gets the full URL path for serving uploaded files
     */
    fun getFileUrl(relativePath: String): String {
        return "/uploads/$relativePath"
    }
}
