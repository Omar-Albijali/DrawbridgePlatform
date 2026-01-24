package uqu.drawbridge.platform.model

import jakarta.persistence.*
import uqu.drawbridge.platform.UserRole
import java.time.LocalDateTime
import com.fasterxml.jackson.annotation.JsonIgnore


@Entity
@Table(name = "users")
open class User(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    // Account Information
    @Column(nullable = false, unique = true)
    open var email: String,

    @Column(nullable = false)
    open var passwordHash: String,

    @Column(nullable = false)
    open var phoneNumber: String,

    @Enumerated(EnumType.STRING)
    open var role: UserRole,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    // Representative Information (Embedded)
    @Embedded
    open var representative: Representative = Representative(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false)
    open var addresses: MutableList<Address> = mutableListOf(),

    // Business info

    @Column(nullable = false)
    open var businessName: String,

    @Column(nullable = true)
    open var avatar: String? = null,

    @Column(nullable = false)
    open var verificationStatus: Boolean,

    @Column(nullable = false)
    open var commercialRegistrationNumber: String

)



@Entity
@Table(name = "admins")
open class Admin(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var name: String,

    @Column(nullable = false, unique = true)
    open var email: String,

    @Column(nullable = false)
    open var passwordHash: String,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    open var role: String 
)

@Entity
@Table(name = "addresses")
open class Address(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var street: String,

    @Column(nullable = false)
    open var city: String,

    @Column(nullable = false, name = "address_state")
    open var state: String, // Renamed column to avoid conflicts

    @Column(nullable = false)
    open var zipCode: String,

    @Column(nullable = false)
    open var country: String,

    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    open var userId: String? = null
)

@Embeddable
open class Representative(
    @Column(name = "rep_name")
    open var name: String = "",

    @Column(name = "rep_job_title")
    open var jobTitle: String = "",

    @Column(name = "rep_phone")
    open var phoneNumber: String = "",

    @Column(name = "rep_email")
    open var email: String = ""
)