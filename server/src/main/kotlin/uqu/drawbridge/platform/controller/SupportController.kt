package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.service.SupportService

@RestController
@RequestMapping("/api/support")
class SupportController(
    private val supportService: SupportService
) {

    // ==================== TICKETS ====================

    @GetMapping("/tickets")
    fun getAllTickets(): ResponseEntity<List<SupportTicketDTO>> =
        ResponseEntity.ok(supportService.getAllTicketsDTO())

    @GetMapping("/tickets/{id}")
    fun getTicketById(@PathVariable id: String): ResponseEntity<SupportTicketDTO> {
        val ticket = supportService.getTicketDTOById(id)
        return if (ticket != null) {
            ResponseEntity.ok(ticket)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/tickets/user/{userId}")
    fun getTicketsByUser(@PathVariable userId: String): ResponseEntity<List<SupportTicketDTO>> =
        ResponseEntity.ok(supportService.getTicketsDTOByUserId(userId))

    @PostMapping("/tickets")
    fun createTicket(@RequestBody request: CreateTicketRequest): ResponseEntity<SupportTicketDTO> {
        val ticket = supportService.createTicketDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket)
    }

    @PostMapping("/tickets/{id}/close")
    fun closeTicket(@PathVariable id: String): ResponseEntity<SupportTicketDTO> {
        val ticket = supportService.closeTicketDTO(id)
        return if (ticket != null) {
            ResponseEntity.ok(ticket)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== CHAT ====================

    @GetMapping("/tickets/{id}/chat")
    fun getTicketChats(@PathVariable id: String): ResponseEntity<List<SupportTicketChatDTO>> =
        ResponseEntity.ok(supportService.getChatsDTOByTicketId(id))

    @PostMapping("/tickets/{id}/chat")
    fun addChatMessage(
        @PathVariable id: String,
        @RequestBody request: AddMessageRequest
    ): ResponseEntity<SupportTicketChatDTO> {
        val chat = supportService.addMessageDTO(id, request)
        
        return if (chat != null) {
            ResponseEntity.status(HttpStatus.CREATED).body(chat)
        } else {
            ResponseEntity.notFound().build()
        }
    }

}
