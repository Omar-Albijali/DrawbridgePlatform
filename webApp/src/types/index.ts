import {
    UserRole,
    OrderStatus,
    ShippingMethod,
    PaymentStatus,
    ScheduleType,
    VerificationStatus,
    InventoryStatus,
    NotificationType,
    NotificationEventKey,
    NotificationEntityType,
    NotificationChannel,
    PaymentMethodType,
    SupportTicketCategory,
    SupportTicketStatus,
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
    ForgotPasswordRequest,
    ResetPasswordRequest,
    VerifyEmailRequest,
    ResendVerificationRequest,
    LogoutRequest,

    // Payment DTOs
    PaymentDTO,
    InvoiceDTO,
    PaymentMethodDTO,
    CreatePaymentRequest,
    CreatePaymentMethodRequest,
    CreateInvoiceRequest,
    SupportTicketDTO,
    CreateTicketRequest,

    // Product DTOs
    CreateProductRequest,
    CreateInventoryItemRequest,
    UpdateAutoOrderConfigRequest,
    UpdateOrderTrackingRequest,
    AddToCartRequest,
    UpdateUserProfileRequest,
    ChangePasswordRequest,
    PosIntegrationConfigDTO,
    PosIntegrationConfigUpdateRequest,
    PosIntegrationApiKeyRotateResponse,
    PosIntegrationEventLogDTO,
    MostOrderedProductDTO
} from 'shared';

export {
    UserRole,
    OrderStatus,
    ShippingMethod,
    PaymentStatus,
    ScheduleType,
    VerificationStatus,
    InventoryStatus,
    NotificationType,
    NotificationEventKey,
    NotificationEntityType,
    NotificationChannel,
    PaymentMethodType,
    SupportTicketCategory,
    SupportTicketStatus,
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
    ForgotPasswordRequest,
    ResetPasswordRequest,
    VerifyEmailRequest,
    ResendVerificationRequest,
    LogoutRequest,
    PaymentDTO,
    InvoiceDTO,
    PaymentMethodDTO,
    CreatePaymentRequest,
    CreatePaymentMethodRequest,
    CreateInvoiceRequest,
    SupportTicketDTO,
    CreateTicketRequest,
    CreateProductRequest,
    CreateInventoryItemRequest,
    UpdateAutoOrderConfigRequest,
    AddToCartRequest,
    UpdateOrderTrackingRequest,
    UpdateUserProfileRequest,
    ChangePasswordRequest,
    PosIntegrationConfigDTO,
    PosIntegrationConfigUpdateRequest,
    PosIntegrationApiKeyRotateResponse,
    PosIntegrationEventLogDTO,
    MostOrderedProductDTO
};

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
export type MostOrderedProduct = MostOrderedProductDTO;

type KotlinDataShape<T> = Omit<T, 'copy' | 'hashCode' | 'equals'>;
export type PosIntegrationConfig = KotlinDataShape<PosIntegrationConfigDTO>;
export type PosIntegrationConfigUpdate = KotlinDataShape<PosIntegrationConfigUpdateRequest>;
export type PosIntegrationApiKeyRotate = KotlinDataShape<PosIntegrationApiKeyRotateResponse>;
export type PosIntegrationEventLog = KotlinDataShape<PosIntegrationEventLogDTO>;

export interface SupportTicketChat {
    id: string;
    ticketId: string;
    adminId?: string | null;
    message: string;
    isAdmin: boolean;
    createdAt: string;
}

export interface AddMessageRequest {
    message: string;
    adminId?: string | null;
}

export interface PaginatedResponse<T> {
    content: T[];
    currentPage: number;
    pageSize: number;
    totalPages: number;
    totalElements: number;
    isFirst: boolean;
    isLast: boolean;
}