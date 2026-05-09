# Requirements Document

## Introduction

A full-stack salon management web application built with Angular (frontend) and Spring Boot (backend). The system enables customers to discover and book salon professionals by city, while giving salon owners and admins dedicated dashboards to manage staff, services, and appointments. Four actor types — Admin, Salon Owner, Professional, and Customer — each have a tailored experience with role-based access control enforced via JWT authentication.

## Glossary

- **System**: The salon management web application (Angular frontend + Spring Boot backend)
- **Customer**: A registered end-user who browses professionals and books appointments
- **Professional**: A salon staff member who registers with a city and is auto-assigned to the matching Salon Owner
- **Salon_Owner**: A pre-seeded salon manager responsible for a specific city; cannot self-register
- **Admin**: A fixed-credential superuser with full access to user management and reports
- **JWT**: JSON Web Token used for stateless authentication and role encoding
- **Appointment**: A booking record linking a Customer, Professional, Service, and date/time
- **Payment**: A financial record linked to an Appointment with a method and status
- **Review**: A rating and comment left by a Customer for a Professional after a completed appointment
- **Service**: A salon offering (e.g., Haircut, Facial) with a category, gender target, price, and duration
- **City**: One of the five supported cities — Visakhapatnam, Vijayawada, Hyderabad, Ananthapur, Khammam
- **Role**: One of ADMIN, SALON_OWNER, PROFESSIONAL, or CUSTOMER
- **AuthService**: The backend service responsible for authentication and registration logic
- **AppointmentService**: The backend service responsible for appointment lifecycle management
- **PaymentService**: The backend service responsible for payment processing
- **GlobalExceptionHandler**: The AOP-based controller advice that maps exceptions to HTTP responses

---

## Requirements

### Requirement 1: Customer Registration and Login

**User Story:** As a customer, I want to register and log in with my email and password, so that I can access my personalized dashboard and book appointments.

#### Acceptance Criteria

1. WHEN a customer submits a registration request with name, email, password, and city, THE System SHALL create a new Customer record and return a 201 response with a JWT token and role CUSTOMER
2. WHEN a customer submits a login request with valid email and password, THE System SHALL return a 200 response with a JWT token and role CUSTOMER
3. IF a customer submits a login request with invalid credentials, THEN THE System SHALL return a 401 Unauthorized response with an error message
4. IF a customer attempts to register with an email that already exists, THEN THE System SHALL return a 400 Bad Request response
5. THE System SHALL store all customer passwords as BCrypt hashes with strength 10

---

### Requirement 2: Salon Owner Login

**User Story:** As a salon owner, I want to log in with my pre-seeded credentials, so that I can access my salon dashboard without a self-registration flow.

#### Acceptance Criteria

1. WHEN a salon owner submits a login request with valid email and password, THE System SHALL return a 200 response with a JWT token and role SALON_OWNER
2. IF a salon owner submits a login request with invalid credentials, THEN THE System SHALL return a 401 Unauthorized response
3. THE System SHALL NOT expose a registration endpoint for Salon Owners
4. THE System SHALL pre-seed exactly one Salon Owner per supported city

---

### Requirement 3: Professional Registration

**User Story:** As a professional, I want to register with my city and specialization, so that I am automatically assigned to the salon owner in my city.

#### Acceptance Criteria

1. WHEN a professional submits a registration request with name, email, password, city, and specialization, THE System SHALL look up the Salon Owner matching that city
2. WHEN a matching Salon Owner is found, THE System SHALL create a Professional record linked to that Salon Owner and return a 201 response with a JWT token and role PROFESSIONAL
3. IF no Salon Owner exists for the selected city, THEN THE System SHALL return a 400 Bad Request with the message "No salon available in selected city"
4. THE System SHALL store all professional passwords as BCrypt hashes with strength 10

---

### Requirement 4: Admin Authentication

**User Story:** As an admin, I want to log in with fixed credentials, so that I can access the admin dashboard for user management and reporting.

#### Acceptance Criteria

1. WHEN an admin submits a login request with valid credentials, THE System SHALL return a 200 response with a JWT token and role ADMIN
2. IF an admin submits a login request with invalid credentials, THEN THE System SHALL return a 401 Unauthorized response
3. THE System SHALL NOT expose a registration endpoint for Admins
4. THE System SHALL store admin credentials in environment variables or application properties, not hardcoded in source code

---

### Requirement 5: JWT-Based Role Authorization

**User Story:** As a system operator, I want all API endpoints protected by JWT and role checks, so that users can only access resources appropriate to their role.

#### Acceptance Criteria

1. THE System SHALL include the user's role, userId, email, and optionally city in every issued JWT token
2. WHEN a request is made to a protected endpoint with a valid JWT, THE System SHALL allow access if the token's role matches the required role for that endpoint
3. IF a request is made with an expired or invalid JWT, THEN THE System SHALL return a 401 Unauthorized response
4. IF a request is made with a valid JWT but insufficient role, THEN THE System SHALL return a 403 Forbidden response
5. THE System SHALL enforce that `/api/auth/**` endpoints are publicly accessible without a JWT
6. THE System SHALL enforce that `/api/professionals` (GET) and `/api/services` (GET) are publicly accessible without a JWT
7. THE JWT token SHALL expire after 24 hours

---

### Requirement 6: Browse Professionals by City

**User Story:** As a customer, I want to browse salon professionals filtered by city, so that I can find available professionals near me.

#### Acceptance Criteria

1. WHEN a request is made to GET `/api/professionals` with a city parameter, THE System SHALL return a list of professionals whose city matches the parameter
2. THE System SHALL support pagination on the professional browse endpoint with page and size query parameters
3. WHEN professionals are returned, THE System SHALL include each professional's name, city, specialization, assigned salon, services, and rating
4. THE System SHALL allow unauthenticated access to the professional browse endpoint

---

### Requirement 7: Appointment Booking

**User Story:** As a customer, I want to book an appointment with a professional for a specific service and date/time, so that I can schedule my salon visit.

#### Acceptance Criteria

1. WHEN a customer submits a booking request with customerId, professionalId, serviceId, and dateTime, THE System SHALL create an Appointment record with status PENDING and return a 201 response
2. IF the selected professional already has an appointment at the requested dateTime, THEN THE System SHALL return a 409 Conflict response with the message "Time slot unavailable"
3. WHEN an appointment is created, THE System SHALL associate it with the correct Customer, Professional, and Service records
4. THE System SHALL restrict appointment creation to users with role CUSTOMER

---

### Requirement 8: Appointment Management

**User Story:** As a customer or salon owner, I want to view and manage appointments, so that I can track upcoming and past bookings.

#### Acceptance Criteria

1. WHEN a customer requests their appointments, THE System SHALL return all appointments associated with that customer's ID
2. WHEN a salon owner requests appointments for their salon, THE System SHALL return all appointments for professionals assigned to that owner
3. WHEN a customer cancels an appointment, THE System SHALL update the appointment status to CANCELLED
4. THE Appointment SHALL only transition through valid statuses: PENDING → CONFIRMED → COMPLETED, or any status → CANCELLED
5. IF a request is made to access another user's appointments, THEN THE System SHALL return a 403 Forbidden response

---

### Requirement 9: Payment Processing

**User Story:** As a customer, I want to pay for my appointment using cash, card, or UPI, so that I can complete my booking.

#### Acceptance Criteria

1. WHEN a customer submits a payment request with appointmentId, amount, and method, THE System SHALL create a Payment record linked to the appointment and return a 200 response with payment confirmation
2. THE Payment amount SHALL equal the price of the service associated with the appointment
3. THE System SHALL support payment methods: CASH, CARD, and UPI
4. WHEN a payment is successfully processed, THE System SHALL update the payment status to PAID
5. THE System SHALL restrict payment creation to users with role CUSTOMER

---

### Requirement 10: Reviews

**User Story:** As a customer, I want to leave a rating and comment for a professional after my appointment, so that other customers can make informed decisions.

#### Acceptance Criteria

1. WHEN a customer submits a review with professionalId, rating (1–5), and comment, THE System SHALL create a Review record and associate it with the customer and professional
2. THE System SHALL enforce that the rating value is between 1 and 5 inclusive
3. IF a rating outside the range 1–5 is submitted, THEN THE System SHALL return a 400 Bad Request response
4. THE System SHALL restrict review creation to users with role CUSTOMER

---

### Requirement 11: Salon Owner Dashboard

**User Story:** As a salon owner, I want to manage my salon profile, staff, services, and appointments from a dedicated dashboard, so that I can run my salon efficiently.

#### Acceptance Criteria

1. WHEN a salon owner requests their profile, THE System SHALL return the salon owner's name, salon name, city, email, and phone
2. WHEN a salon owner requests their staff list, THE System SHALL return all professionals assigned to that owner
3. WHEN a salon owner requests service management, THE System SHALL return all services available in the system
4. WHEN a salon owner requests their appointment overview, THE System SHALL return all appointments for professionals in their salon
5. THE System SHALL restrict all `/api/owners/**` endpoints to users with role SALON_OWNER

---

### Requirement 12: Admin Dashboard

**User Story:** As an admin, I want to view all users, manage salon owners, and access reports, so that I can oversee the entire platform.

#### Acceptance Criteria

1. WHEN an admin requests the user list, THE System SHALL return all customers, salon owners, and professionals in the system
2. WHEN an admin requests the salon owner list, THE System SHALL return all pre-seeded salon owners
3. WHEN an admin requests reports, THE System SHALL return a summary of appointments and payments
4. THE System SHALL restrict all `/api/admin/**` endpoints to users with role ADMIN

---

### Requirement 13: Error Handling

**User Story:** As a system operator, I want all errors to return consistent, structured HTTP responses, so that clients can handle failures gracefully.

#### Acceptance Criteria

1. WHEN a ResourceNotFoundException is thrown, THE GlobalExceptionHandler SHALL return a 404 Not Found response with a structured error body
2. WHEN an AccessDeniedException is thrown, THE GlobalExceptionHandler SHALL return a 403 Forbidden response with a structured error body
3. WHEN a ValidationException is thrown, THE GlobalExceptionHandler SHALL return a 400 Bad Request response with a structured error body
4. WHEN a ConflictException is thrown, THE GlobalExceptionHandler SHALL return a 409 Conflict response with a structured error body
5. WHEN any unhandled exception is thrown, THE GlobalExceptionHandler SHALL return a 500 Internal Server Error response

---

### Requirement 14: Security and Data Protection

**User Story:** As a system operator, I want the application to follow security best practices, so that user data and access are protected.

#### Acceptance Criteria

1. THE System SHALL configure CORS to allow requests only from the Angular frontend origin
2. THE System SHALL prevent SQL injection by using JPA parameterized queries exclusively
3. THE System SHALL enforce HTTPS in production deployment
4. THE System SHALL index the `city` column on the Professional and Customer tables for query performance
5. THE System SHALL use Angular lazy loading per dashboard module to reduce initial bundle size

---

### Requirement 15: Seed Data Initialization

**User Story:** As a system operator, I want the database to be pre-seeded with salon owners and services, so that the application is functional immediately after deployment.

#### Acceptance Criteria

1. THE System SHALL seed exactly five Salon Owner records, one for each supported city (Visakhapatnam, Vijayawada, Hyderabad, Ananthapur, Khammam)
2. THE System SHALL seed service records for MEN, WOMEN, and KIDS categories covering Hair, Beard, Skin, Nails, Makeup, Body, and Grooming categories
3. WHEN the application starts, THE System SHALL ensure seed data is present before accepting requests
