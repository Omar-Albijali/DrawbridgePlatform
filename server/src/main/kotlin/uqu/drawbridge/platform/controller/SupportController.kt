package uqu.drawbridge.platform.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.service.SupportService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/support")
class SupportController(
    private val supportService: SupportService,
    private val userService: UserService
) {
    private fun getCurrentUser(authentication: Authentication) =
        userService.getUserByEmail(authentication.name)
            ?: throw NoSuchElementException("User not found")

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createTicket(
        authentication: Authentication,
        @RequestParam subject: String,
        @RequestParam category: SupportTicketCategory,
        @RequestParam description: String,
        @RequestParam(name = "attachment", required = false) attachment: MultipartFile?
    ): ResponseEntity<SupportTicketDTO> {
        val user = getCurrentUser(authentication)
        val created = supportService.createTicket(
            user = user,
            subject = subject,
            category = category,
            description = description,
            attachment = attachment
        )
        return ResponseEntity.status(201).body(created)
    }

    @GetMapping("/my")
    fun getMyTickets(authentication: Authentication): ResponseEntity<List<SupportTicketDTO>> {
        val user = getCurrentUser(authentication)
        return ResponseEntity.ok(supportService.getMyTickets(user.id!!))
    }

    @GetMapping("/{id}")
    fun getTicketById(
        authentication: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<SupportTicketDTO> {
        val user = getCurrentUser(authentication)
        return ResponseEntity.ok(supportService.getTicketById(id, user.id!!))
    }
}
