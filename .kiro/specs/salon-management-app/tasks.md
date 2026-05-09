# Tasks

## Task List

- [x] 1. Project Scaffolding and Configuration
  - [x] 1.1 Initialize Spring Boot project with dependencies: web, data-jpa, security, aop, validation, jjwt, lombok, modelmapper, springdoc-openapi (Swagger), mysql/postgresql driver
  - [x] 1.2 Initialize Angular project with dependencies: bootstrap, @angular/router, @angular/forms (ReactiveFormsModule), @angular/common/http, cypress
  - [x] 1.3 Configure application.properties with database connection, JWT secret, CORS origin, and server port using environment variables
  - [x] 1.4 Configure Spring Security base setup with JWT filter chain and public route exceptions
  - [x] 1.5 Configure ModelMapper bean in Spring Boot for entity-to-DTO and DTO-to-entity conversion (entities must never be exposed directly to APIs)
  - [x] 1.6 Configure Swagger/OpenAPI (springdoc-openapi) with base URI mapping and JWT bearer auth scheme

- [x] 2. Database Schema and Seed Data
  - [x] 2.1 Create SQL schema for Admin, SalonOwner, Professional, Customer, Services, Appointments, Payments, Reviews tables with all constraints and foreign keys
  - [x] 2.2 Add city index on Professional and Customer tables for query performance
  - [x] 2.3 Create seed data script for 5 SalonOwner records (one per city: Visakhapatnam, Vijayawada, Hyderabad, Ananthapur, Khammam) with BCrypt-hashed passwords
  - [x] 2.4 Create seed data script for Services (MEN, WOMEN, KIDS categories covering Hair, Beard, Skin, Nails, Makeup, Body, Grooming, Special)
  - [x] 2.5 Create JPA entity classes using Lombok (@Data, @Builder, @NoArgsConstructor) for all tables with appropriate generation strategies
  - [x] 2.6 Create Spring Data JPA repositories for all entities

- [x] 3. DTOs and Mapping Layer
  - [x] 3.1 Create request DTOs for all API inputs: CustomerRegisterRequest, LoginRequest, ProfessionalRegisterRequest, AppointmentRequest, PaymentRequest, ReviewRequest
  - [x] 3.2 Create response DTOs for all API outputs: AuthResponse, CustomerResponse, ProfessionalResponse, SalonOwnerResponse, AppointmentResponse, PaymentResponse, ReviewResponse, ServiceResponse
  - [x] 3.3 Add Bean validation annotations (@NotNull, @NotBlank, @Email, @Min, @Max, @Size) to all request DTOs; null/empty fields must return "Please provide a valid <attribute name>" error message
  - [x] 3.4 Implement custom validators for complex rules: valid city enum, rating range 1–5
  - [x] 3.5 Configure ModelMapper mappings for all entity↔DTO conversions

- [x] 4. Logging and Exception Handling (AOP)
  - [x] 4.1 Implement LoggingAspect using @Aspect and @Around to log all service method entry/exit with method name, arguments, and execution time
  - [x] 4.2 Create custom exception classes: ResourceNotFoundException, ConflictException, ValidationException, UnauthorizedException
  - [x] 4.3 Implement GlobalExceptionHandler (@ControllerAdvice) mapping: ResourceNotFoundException → 404, AccessDeniedException → 403, ValidationException → 400, ConflictException → 409, MethodArgumentNotValidException → 400 with field-level messages, all others → 500
  - [x] 4.4 Write JUnit test: ResourceNotFoundException produces a 404 response with non-empty error body

- [x] 5. JWT Utility and Security
  - [x] 5.1 Implement JwtUtil class for token generation, parsing, and validation (role, userId, email, city claims; 24-hour expiry)
  - [x] 5.2 Implement JwtAuthenticationFilter to extract and validate JWT from Authorization header on each request
  - [x] 5.3 Configure Spring Security route guards per the access control table (PUBLIC, CUSTOMER, SALON_OWNER, PROFESSIONAL, ADMIN)
  - [x] 5.4 Write JUnit test: generated JWT contains role, userId, and email claims

- [x] 6. Authentication Endpoints
  - [x] 6.1 Implement POST /api/auth/customer/register — validate DTO with Bean validation, hash password (BCrypt strength 10), insert Customer, return JWT with role CUSTOMER
  - [x] 6.2 Implement POST /api/auth/customer/login — validate credentials, return JWT with role CUSTOMER
  - [x] 6.3 Implement POST /api/auth/owner/login — validate against pre-seeded SalonOwner, return JWT with role SALON_OWNER (no registration endpoint)
  - [x] 6.4 Implement POST /api/auth/professional/register — validate DTO, look up SalonOwner by city, insert Professional linked to owner, return JWT with role PROFESSIONAL
  - [x] 6.5 Implement POST /api/auth/admin/login — validate against Admin record, return JWT with role ADMIN (no registration endpoint)
  - [x] 6.6 Write JUnit test: customer register then login returns same userId and role CUSTOMER
  - [x] 6.7 Write JUnit test: unregistered email/password returns 401 on login
  - [x] 6.8 Write JUnit test: duplicate email registration returns 400
  - [x] 6.9 Write JUnit test: professional registered with city X is assigned to SalonOwner of city X
  - [x] 6.10 Write JUnit test: invalid city on professional registration returns 400 with correct message

- [x] 7. Professional Profile and Services API
  - [x] 7.1 Implement GET /api/professionals?city=X&page=0&size=10 — filter by city with pagination, return ProfessionalResponse with name, city, specialization, salon, services, rating
  - [x] 7.2 Implement GET /api/professionals/{id} — return full professional profile
  - [x] 7.3 Implement PUT /api/professionals/{id}/profile — allow professional to update name, specialization, experience (PROFESSIONAL role only)
  - [x] 7.4 Implement GET /api/services — return all seeded services grouped by gender/category (public endpoint)
  - [x] 7.5 Write JUnit test: professionals filtered by city all have matching city in response
  - [x] 7.6 Write JUnit test: returned list size is at most the requested page size
  - [x] 7.7 Write JUnit test: unauthenticated GET /api/services returns 200 with service list

- [x] 8. Appointment API
  - [x] 8.1 Implement POST /api/appointments — validate DTO with Bean validation, check for time slot conflict, create Appointment with status PENDING
  - [x] 8.2 Implement GET /api/appointments?customerId=X — return appointments for the authenticated customer only
  - [x] 8.3 Implement GET /api/appointments?salonOwnerId=X — return appointments for all professionals under the salon owner
  - [x] 8.4 Implement PATCH /api/appointments/{id}/status — update status with transition validation (PENDING → CONFIRMED → COMPLETED; any → CANCELLED)
  - [x] 8.5 Implement PATCH /api/appointments/{id}/cancel — shortcut to set status CANCELLED
  - [x] 8.6 Write JUnit test: valid booking creates appointment with PENDING status and correct associations
  - [x] 8.7 Write JUnit test: booking same professional at same dateTime returns 409
  - [x] 8.8 Write JUnit test: customer appointment query returns only that customer's appointments
  - [x] 8.9 Write JUnit test: salon owner appointment query returns only appointments for their professionals
  - [x] 8.10 Write JUnit test: invalid status transitions are rejected
  - [x] 8.11 Write JUnit test: accessing another user's appointments returns 403

- [x] 9. Payment API
  - [x] 9.1 Implement POST /api/payments — validate amount equals service price, create Payment record with status PAID, return receipt
  - [x] 9.2 Implement GET /api/payments/{appointmentId} — return payment details for an appointment
  - [x] 9.3 Write JUnit test: payment amount must equal the price of the service on the linked appointment
  - [x] 9.4 Write JUnit test: payment with each supported method (CASH, CARD, UPI) succeeds

- [x] 10. Review API
  - [x] 10.1 Implement POST /api/reviews — validate rating 1–5 with Bean validation, create Review linked to customer and professional
  - [x] 10.2 Implement GET /api/reviews?professionalId=X — return reviews and average rating for a professional
  - [x] 10.3 Write JUnit test: rating outside 1–5 returns 400; rating within 1–5 is accepted
  - [x] 10.4 Write JUnit test: created review references correct customerId and professionalId

- [x] 11. Salon Owner API
  - [x] 11.1 Implement GET /api/owners/{id}/profile — return salon owner profile (name, salon name, city, email, phone)
  - [x] 11.2 Implement GET /api/owners/{id}/staff — return professionals assigned to this owner
  - [x] 11.3 Implement GET /api/owners/{id}/appointments — return all appointments for the salon (delegates to AppointmentService)
  - [x] 11.4 Write JUnit test: staff list contains only professionals with matching salonOwnerId

- [x] 12. Admin API
  - [x] 12.1 Implement GET /api/admin/users — return all customers, owners, and professionals
  - [x] 12.2 Implement GET /api/admin/owners — return all pre-seeded salon owners
  - [x] 12.3 Implement GET /api/admin/reports — return summary of appointments and payments
  - [x] 12.4 Write JUnit test: admin user list contains all registered users across all roles

- [x] 13. Backend Unit Tests (JUnit + Mockito)
  - [x] 13.1 Write JUnit 5 + Mockito unit tests for AuthService covering all register and login methods
  - [x] 13.2 Write JUnit 5 + Mockito unit tests for AppointmentService covering create, cancel, and status transition methods
  - [x] 13.3 Write JUnit 5 + Mockito unit tests for PaymentService covering process and validation methods
  - [x] 13.4 Write JUnit 5 + Mockito unit tests for ReviewService covering create and rating validation
  - [x] 13.5 Achieve minimum 80% code coverage across all service classes

- [x] 14. Angular Models and Services
  - [x] 14.1 Create TypeScript interfaces in model folder: AuthResponse, Customer, SalonOwner, Professional, Service, Appointment, Payment, Review, Role, City
  - [x] 14.2 Create AuthService with loginCustomer, loginOwner, loginAdmin, registerCustomer, registerProfessional, logout, getRole, getToken methods using HttpClient
  - [x] 14.3 Implement JWT storage in localStorage and HTTP interceptor to attach Authorization header to all outgoing requests
  - [x] 14.4 Create ProfessionalService with getByCity (with pagination), getById, getServices methods
  - [x] 14.5 Create AppointmentService with book, getByCustomer, getBySalon, cancel methods
  - [x] 14.6 Create PaymentService with process and getByAppointment methods
  - [x] 14.7 Create ReviewService with submit and getByProfessional methods

- [x] 15. Angular Route Guards and Routing
  - [x] 15.1 Implement AuthGuard with role-based canActivate checks for CUSTOMER, SALON_OWNER, ADMIN, PROFESSIONAL routes
  - [x] 15.2 Configure AppRoutingModule with lazy-loaded feature modules for auth, customer, owner, admin
  - [x] 15.3 Add redirect logic: unauthenticated users → login page; authenticated users → role-specific dashboard

- [x] 16. Angular Auth Forms with Reactive Validation
  - [x] 16.1 Create CustomerRegisterComponent using ReactiveFormsModule with validators: name (required, minLength 2), email (required, email format), password (required, minLength 8), city (required, must be one of 5 valid cities); show inline error messages on touched/dirty fields
  - [x] 16.2 Create CustomerLoginComponent using ReactiveFormsModule with validators: email (required, email format), password (required); disable submit button when form is invalid
  - [x] 16.3 Create OwnerLoginComponent using ReactiveFormsModule with validators: email (required, email format), password (required); no registration link
  - [x] 16.4 Create AdminLoginComponent using ReactiveFormsModule with validators: username (required), password (required)
  - [x] 16.5 Create ProfessionalRegisterComponent using ReactiveFormsModule with validators: name (required, minLength 2), email (required, email format), password (required, minLength 8), city (required, valid city enum), specialization (required); show inline error messages
  - [x] 16.6 Implement custom Angular validator for city dropdown: must be one of Visakhapatnam, Vijayawada, Hyderabad, Ananthapur, Khammam
  - [x] 16.7 Implement password strength validator: minimum 8 characters, at least one uppercase, one number

- [x] 17. Angular Customer Dashboard
  - [x] 17.1 Create ProfessionalBrowseComponent — card grid with CityFilterComponent, calls GET /api/professionals with pagination; show loading spinner during fetch
  - [x] 17.2 Create AppointmentBookingComponent — ReactiveForm with date/time picker (validator: date must not be in the past), service selector (required), professional selector (required); calls POST /api/appointments
  - [x] 17.3 Create AppointmentHistoryComponent — tabbed list of upcoming/past appointments with rebook and cancel actions
  - [x] 17.4 Create CustomerProfileComponent — ReactiveForm for view/edit profile with validators: phone (pattern: 10 digits), city (required, valid city)

- [x] 18. Angular Salon Owner Dashboard
  - [x] 18.1 Create OwnerProfileComponent — displays salon name, city, contact info
  - [x] 18.2 Create StaffListComponent — lists assigned professionals with their specializations and ratings
  - [x] 18.3 Create ServiceManagementComponent — view all services grouped by gender/category
  - [x] 18.4 Create AppointmentOverviewComponent — table of all salon appointments with status badges

- [x] 19. Angular Admin Dashboard
  - [x] 19.1 Create UserManagementComponent — Bootstrap table of all users with role badges
  - [x] 19.2 Create SalonOwnerListComponent — table of pre-seeded owners with city info
  - [x] 19.3 Create ReportsComponent — summary cards for total appointments and payments

- [x] 20. Angular Shared Components
  - [x] 20.1 Create NavbarComponent — role-aware navigation links, logout button, themed with #244AFD primary color and white text
  - [x] 20.2 Create CityFilterComponent — reusable dropdown bound to the 5 supported cities, emits selected city via EventEmitter
  - [x] 20.3 Create ProfessionalCardComponent — Bootstrap card showing professional name, specialization, city, rating stars, and services list
  - [x] 20.4 Create LoadingSpinnerComponent — centered Bootstrap spinner shown during HTTP calls
  - [x] 20.5 Create AlertComponent — Bootstrap toast for success/error messages with auto-dismiss after 4 seconds

- [ ] 21. Integration and E2E Testing
  - [ ] 21.1 Write Spring Boot integration tests with H2 in-memory DB for each controller (auth, professionals, appointments, payments, reviews, admin, owners)
  - [ ] 21.2 Write Angular TestBed integration tests for AuthService and AppointmentService with mocked HttpClient
  - [ ] 21.3 Write Cypress E2E test: customer registration → browse professionals by city → book appointment → process payment
  - [ ] 21.4 Write Cypress E2E test: professional registration → auto-assignment to salon owner verified in UI
  - [ ] 21.5 Write Cypress E2E test: salon owner login → view staff list → view appointment overview
