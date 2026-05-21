package uqu.drawbridge.platform.model

import jakarta.persistence.*
import uqu.drawbridge.platform.UserRole
import java.time.LocalDateTime
import com.fasterxml.jackson.annotation.JsonIgnore


@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    // Account Information
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false)
    var phoneNumber: String,

    @Enumerated(EnumType.STRING)
    var role: UserRole,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    // Representative Information (Embedded)
    @Embedded
    var representative: Representative = Representative(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false)
    var addresses: MutableList<Address> = mutableListOf(),

    // Business info

    @Column(nullable = false)
    var businessName: String,

    @Column(nullable = true)
    var avatar: String? = null,

    @Column(nullable = false)
    var verificationStatus: Boolean,

    @Column(nullable = false)
    var commercialRegistrationNumber: String

)



@Entity
@Table(name = "admins")
class Admin(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var role: String 
)

@Entity
@Table(name = "addresses")
class Address(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var street: String,

    @Column(nullable = false)
    var city: String,

    @Column(nullable = false, name = "address_state")
    var state: String, // Renamed column to avoid conflicts

    @Column(nullable = false)
    var zipCode: String,

    @Column(nullable = false)
    var country: String,

    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    var userId: String? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null
)

@Embeddable
class Representative(
    @Column(name = "rep_name")
    var name: String = "",

    @Column(name = "rep_job_title")
    var jobTitle: String = "",

    @Column(name = "rep_phone")
    var phoneNumber: String = "",

    @Column(name = "rep_email")
    var email: String = ""
)
