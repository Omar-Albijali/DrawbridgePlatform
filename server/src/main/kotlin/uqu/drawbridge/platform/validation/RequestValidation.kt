package uqu.drawbridge.platform.validation

import java.math.BigDecimal

object RequestValidation {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun requireNotBlank(value: String, fieldName: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "$fieldName is required" }
        return normalized
    }

    fun requireEmail(value: String, fieldName: String = "email"): String {
        val normalized = requireNotBlank(value, fieldName).lowercase()
        require(emailRegex.matches(normalized)) { "$fieldName must be a valid email address" }
        return normalized
    }

    fun requireMinLength(value: String, fieldName: String, minLength: Int): String {
        val normalized = requireNotBlank(value, fieldName)
        require(normalized.length >= minLength) { "$fieldName must be at least $minLength characters" }
        return normalized
    }

    fun requirePositive(value: Int, fieldName: String): Int {
        require(value > 0) { "$fieldName must be greater than zero" }
        return value
    }

    fun requireNonNegative(value: Int, fieldName: String): Int {
        require(value >= 0) { "$fieldName must be zero or greater" }
        return value
    }

    fun requireNonNegative(value: Double, fieldName: String): Double {
        require(value >= 0.0) { "$fieldName must be zero or greater" }
        return value
    }

    fun parsePositiveBigDecimal(value: String, fieldName: String): BigDecimal {
        val normalized = requireNotBlank(value, fieldName)
        val parsed = normalized.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("$fieldName must be a valid decimal number")
        require(parsed > BigDecimal.ZERO) { "$fieldName must be greater than zero" }
        return parsed
    }
}
