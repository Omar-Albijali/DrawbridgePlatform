// Import all shared types from the shared module
import {
    // Enums
    UserRole,
    OrderStatus,
    ShippingMethod,
    PaymentStatus,
    ScheduleType,
    VerificationStatus,
    InventoryStatus,
    NotificationType,
    TicketStatus,
    NotificationChannel,
    PaymentMethodType,

    // DTOs
    OrderDTO,
    OrderItemDTO,
    OrderGroupDTO,
    UserDTO,
    ProductDTO,
    InventoryItemDTO,
    NotificationDTO,
    CartItemDTO,
    ShoppingCartDTO,
    TrackingInfoDTO,
    AutoOrderConfigDTO,
    CategoryDTO,
    AddressDto,
    RepresentativeDto,
    LoginRequest,
    RegisterRequest,
    AuthResponse,
    ErrorResponse,
    ImageUploadResponse,
    ProductImageResponse,
    AddressResponseDto,
    CreateAddressRequest,

    // Payment DTOs
    PaymentDTO,
    InvoiceDTO,
    PaymentMethodDTO,
    CreatePaymentRequest,
    CreatePaymentMethodRequest,
    CreateInvoiceRequest,

    // Support DTOs
    SupportTicketDTO,
    SupportTicketChatDTO,
    CreateTicketRequest,
    AddMessageRequest,

    // Product DTOs
    CreateProductRequest,

    // Inventory Request DTOs
    CreateInventoryItemRequest,
    UpdateAutoOrderConfigRequest,

    // Order Request DTOs
    UpdateOrderTrackingRequest,

    // Cart Request DTOs
    AddToCartRequest,

    // User Request DTOs
    UpdateUserProfileRequest
} from 'shared';

// Re-export all shared enums and types for use throughout the webapp
export {
    UserRole,
    OrderStatus,
    ShippingMethod,
    PaymentStatus,
    ScheduleType,
    VerificationStatus,
    InventoryStatus,
    NotificationType,
    TicketStatus,
    NotificationChannel,
    PaymentMethodType,
    OrderDTO,
    OrderItemDTO,
    OrderGroupDTO,
    UserDTO,
    ProductDTO,
    InventoryItemDTO,
    NotificationDTO,
    CartItemDTO,
    ShoppingCartDTO,
    TrackingInfoDTO,
    AutoOrderConfigDTO,
    CategoryDTO,
    AddressDto,
    RepresentativeDto,
    LoginRequest,
    RegisterRequest,
    AuthResponse,
    ErrorResponse,
    ImageUploadResponse,
    ProductImageResponse,
    AddressResponseDto,
    CreateAddressRequest,
    PaymentDTO,
    InvoiceDTO,
    PaymentMethodDTO,
    CreatePaymentRequest,
    CreatePaymentMethodRequest,
    CreateInvoiceRequest,
    SupportTicketDTO,
    SupportTicketChatDTO,
    CreateTicketRequest,
    AddMessageRequest,
    CreateProductRequest,
    CreateInventoryItemRequest,
    UpdateAutoOrderConfigRequest,
    AddToCartRequest,
    UpdateOrderTrackingRequest,
    UpdateUserProfileRequest
};

// Define type aliases to match the shared DTO structures
// This allows the rest of the application to continue using generic names
export type User = UserDTO;
export type Product = ProductDTO;
export type InventoryItem = InventoryItemDTO;
export type Order = OrderDTO;
export type OrderItem = OrderItemDTO;
export type OrderGroup = OrderGroupDTO;
export type Notification = NotificationDTO;
export type CartItem = CartItemDTO;
export type ShoppingCart = ShoppingCartDTO;
export type TrackingInfo = TrackingInfoDTO;
export type AutoOrderConfig = AutoOrderConfigDTO;
export type Category = CategoryDTO;
export type Address = AddressDto;
export type Representative = RepresentativeDto;
export type Payment = PaymentDTO;
export type Invoice = InvoiceDTO;
export type PaymentMethod = PaymentMethodDTO;
export type SupportTicket = SupportTicketDTO;
export type SupportTicketChat = SupportTicketChatDTO;
