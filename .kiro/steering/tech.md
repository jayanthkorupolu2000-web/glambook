# Tech Stack

## Backend

- **Language**: Java 17
- **Framework**: Spring Boot 3.1.5
- **Build Tool**: Maven (`backend/pom.xml`)
- **Database**: MySQL 8 (schema: `glambook`), H2 for tests
- **ORM**: Spring Data JPA / Hibernate (ddl-auto: update)
- **Security**: Spring Security + JWT (jjwt 0.11.5), BCrypt password hashing
- **Mapping**: ModelMapper 3.1.1
- **Validation**: Spring Validation (`@Valid`, custom `@ValidCity` annotation)
- **AOP**: Spring AOP (`LoggingAspect` for cross-cutting logging)
- **API Docs**: SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`, `/v3/api-docs`)
- **Property-Based Testing**: jqwik 1.7.4 + jqwik-spring 0.9.0

## Frontend

- **Language**: TypeScript 5.1
- **Framework**: Angular 16
- **UI**: Bootstrap 5.3
- **HTTP**: Angular `HttpClient` with JWT interceptor
- **Routing**: Angular Router with `AuthGuard` / `NoAuthGuard`
- **Property-Based Testing**: fast-check 3.12
- **E2E Testing**: Cypress 13

## Key Configuration

- Backend runs on port `8080` (env: `SERVER_PORT`)
- Frontend dev server on port `4200`
- CORS allowed origin: `http://localhost:4200` (env: `CORS_ORIGIN`)
- JWT secret via env `JWT_SECRET`, expiry 24h
- File uploads stored in `uploads/` directory
- DB credentials via env `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Common Commands

### Backend (run from `backend/`)
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test

# Skip tests during build
mvn clean install -DskipTests
```

### Frontend (run from `frontend/`)
```bash
# Install dependencies
npm install

# Dev server (http://localhost:4200)
ng serve

# Build for production
ng build

# Unit tests
ng test

# E2E tests
cypress open
```
