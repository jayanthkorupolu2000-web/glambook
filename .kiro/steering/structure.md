# Project Structure

## Root Layout

```
/
├── backend/          # Spring Boot REST API
├── frontend/         # Angular SPA
└── .kiro/
    ├── specs/        # Feature specs and implementation plans
    └── steering/     # Project guidance documents
```

## Backend Structure (`backend/src/main/java/com/salon/`)

Follows standard Spring Boot layered architecture:

```
com.salon/
├── SalonManagementApplication.java   # Main entry point
├── aspect/                            # AOP cross-cutting concerns (LoggingAspect)
├── config/                            # Configuration classes
│   ├── AppConfig.java                 # ModelMapper, CORS
│   ├── SecurityConfig.java            # JWT filter chain, BCrypt encoder
│   └── OpenApiConfig.java             # Swagger/OpenAPI setup
├── controller/                        # REST endpoints (30+ controllers)
│   ├── AuthController.java            # /api/auth/** (login, register)
│   ├── AdminController.java           # /api/admin/**
│   ├── CustomerController.java        # /api/customer/**
│   ├── ProfessionalController.java    # /api/professional/**
│   ├── SalonOwnerController.java      # /api/owner/**
│   └── ...                            # Domain-specific controllers
├── dto/
│   ├── request/                       # Request DTOs (e.g., LoginRequest, RegisterRequest)
│   └── response/                      # Response DTOs (e.g., AuthResponse, AppointmentResponse)
├── entity/                            # JPA entities (50+ domain models)
│   ├── Customer.java, Professional.java, SalonOwner.java, Admin.java
│   ├── Appointment.java, Payment.java, Review.java, Service.java
│   ├── *Status.java, *Type.java       # Enums (AppointmentStatus, PaymentMethod, etc.)
│   └── ...
├── exception/                         # Custom exceptions + GlobalExceptionHandler
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice for centralized error handling
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── ...
├── repository/                        # Spring Data JPA repositories
│   ├── CustomerRepository.java
│   ├── AppointmentRepository.java
│   └── ...                            # One per entity
├── security/                          # JWT authentication
│   ├── JwtUtil.java                   # Token generation/validation
│   └── JwtAuthenticationFilter.java   # Filter for extracting/validating tokens
├── service/                           # Business logic interfaces
│   ├── AuthService.java, AppointmentService.java, PaymentService.java
│   └── impl/                          # Service implementations
│       ├── AuthServiceImpl.java
│       └── ...
└── validation/                        # Custom validators
    ├── ValidCity.java                 # Annotation for city validation
    └── CityValidator.java             # Validator logic
```

### Backend Naming Conventions

- **Controllers**: `{Domain}Controller.java` (e.g., `AppointmentController`)
- **Services**: `{Domain}Service.java` interface + `{Domain}ServiceImpl.java` implementation
- **Repositories**: `{Entity}Repository.java` (extends `JpaRepository`)
- **Entities**: Singular noun (e.g., `Customer`, `Appointment`)
- **DTOs**: `{Purpose}Request.java` / `{Purpose}Response.java`
- **Enums**: Descriptive name ending in `Status`, `Type`, or standalone (e.g., `AppointmentStatus`, `Gender`)

## Frontend Structure (`frontend/src/app/`)

Follows Angular feature-based modular architecture:

```
app/
├── app.module.ts                      # Root module
├── app-routing.module.ts              # Top-level routes
├── app.component.*                    # Root component
├── features/                          # Feature modules (role-based)
│   ├── admin/                         # Admin dashboard & management
│   ├── auth/                          # Login, register components
│   ├── customer/                      # Customer dashboard, bookings, reviews
│   ├── owner/                         # Salon owner dashboard, staff, reports
│   └── professional/                  # Professional dashboard, availability, analytics
├── guards/                            # Route guards
│   ├── auth.guard.ts                  # Protects authenticated routes
│   └── no-auth.guard.ts               # Redirects logged-in users from login/register
├── interceptors/
│   └── auth.interceptor.ts            # Attaches JWT to outgoing requests
├── models/                            # TypeScript interfaces/types
│   ├── customer.model.ts
│   ├── professional.model.ts
│   ├── complaint.model.ts
│   └── index.ts                       # Barrel export
├── services/                          # HTTP services (API calls)
│   ├── auth.service.ts                # Login, register, token management
│   ├── appointment.service.ts
│   ├── payment.service.ts
│   └── ...                            # One per domain
├── shared/                            # Shared components, pipes, directives
│   ├── components/                    # Reusable UI components
│   └── shared.module.ts
└── validators/                        # Custom form validators
    ├── city.validator.ts
    └── password-strength.validator.ts
```

### Frontend Naming Conventions

- **Components**: `{name}.component.ts` (e.g., `login.component.ts`)
- **Services**: `{domain}.service.ts` (e.g., `auth.service.ts`)
- **Guards**: `{name}.guard.ts` (e.g., `auth.guard.ts`)
- **Models**: `{entity}.model.ts` (e.g., `customer.model.ts`)
- **Feature modules**: Organized by actor role (`admin/`, `customer/`, `professional/`, `owner/`)

## Key Architectural Patterns

### Backend

- **Layered Architecture**: Controller → Service → Repository → Entity
- **DTO Pattern**: Separate request/response DTOs from entities
- **Repository Pattern**: Spring Data JPA repositories for data access
- **Exception Handling**: Centralized via `@ControllerAdvice` (`GlobalExceptionHandler`)
- **Security**: JWT filter chain with role-based access control
- **Validation**: `@Valid` on controller methods + custom validators

### Frontend

- **Feature Modules**: Role-based feature separation (admin, customer, professional, owner)
- **Service Layer**: HTTP services encapsulate API calls
- **Guards**: Route protection based on authentication state
- **Interceptors**: Automatic JWT attachment to requests
- **Reactive Forms**: Form validation with custom validators

## Database Schema

- Managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
- Entities use JPA annotations (`@Entity`, `@Table`, `@ManyToOne`, etc.)
- Enums stored as strings (`@Enumerated(EnumType.STRING)`)
- Relationships: `@ManyToOne`, `@OneToMany`, `@OneToOne` with appropriate cascade/fetch strategies
