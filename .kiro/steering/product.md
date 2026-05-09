# Product: GlamBook – Salon Management Web Application

GlamBook is a full-stack salon management platform that connects customers with salon professionals across five cities in Andhra Pradesh/Telangana (Visakhapatnam, Vijayawada, Hyderabad, Ananthapur, Khammam).

## Four Actor Roles

- **Customer** – Registers, browses professionals by city, books appointments, makes payments, leaves reviews, manages a loyalty program, and orders products.
- **Professional** – Registers with a city (auto-assigned to that city's salon owner), manages availability, portfolio, consultations, and views analytics.
- **Salon Owner** – Pre-seeded per city (no self-registration). Manages staff, services, promotions, resources, complaints, and reports.
- **Admin** – Fixed-credential superuser. Manages all users, views platform-wide analytics, and handles escalations.

## Core Features

- JWT-based role-aware authentication (ADMIN, SALON_OWNER, PROFESSIONAL, CUSTOMER)
- Appointment booking with status lifecycle and payment processing
- Loyalty points, tiers, and redemption
- Group bookings, consultations, and communications
- Review/rating system with professional responses
- Product catalog and order management
- Complaint management with feedback loop
- File uploads (portfolio images, before/after photos)
- Swagger UI for API documentation
