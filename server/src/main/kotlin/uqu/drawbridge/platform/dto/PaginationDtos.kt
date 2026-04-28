package uqu.drawbridge.platform.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PaginatedResponse<T>(
    val content: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalElements: Long,
    @JsonProperty("isFirst")
    val first: Boolean,
    @JsonProperty("isLast")
    val last: Boolean
)
