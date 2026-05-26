package uqu.drawbridge.posdemo.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import uqu.drawbridge.platform.dto.PosInventoryChangeRequest
import uqu.drawbridge.platform.dto.PosInventoryChangeResponse

class PosDemoApiClient {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun sendInventoryChange(
        serverUrl: String,
        apiKey: String,
        request: PosInventoryChangeRequest
    ): Result<PosInventoryChangeResponse> {
        return try {
            val endpoint = serverUrl.trimEnd('/') + "/api/pos/inventory/changes"
            
            val response: HttpResponse = client.post(endpoint) {
                header("X-API-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess() || response.status.value == 202) {
                val bodyResult: PosInventoryChangeResponse = response.body()
                Result.success(bodyResult)
            } else {
                Result.failure(Exception("HTTP Error ${response.status.value}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
