# SalonOwnerListComponent

## Overview
The SalonOwnerListComponent displays all pre-seeded salon owners in a Bootstrap table with city information. This component is part of the Angular Admin Dashboard module.

## Features
- Displays salon owner details including name, salon name, city, email, and phone
- Calls the admin API endpoint GET /api/admin/owners
- Uses proper Bootstrap styling with the app theme (#244AFD primary color)
- Includes proper error handling and loading states
- Color-coded city badges for easy identification
- Responsive table design

## API Integration
- **Endpoint**: `GET /api/admin/owners`
- **Service**: `AdminService.getAllOwners()`
- **Response**: Array of `SalonOwner` objects

## Styling
- Uses Bootstrap 5 classes for responsive design
- Custom SCSS for theme colors (#244AFD)
- Color-coded badges for different cities:
  - Visakhapatnam: Primary blue
  - Vijayawada: Success green
  - Hyderabad: Info cyan
  - Ananthapur: Warning yellow
  - Khammam: Secondary gray

## Navigation
- Accessible via `/admin/owners` route
- Integrated into admin dashboard with navigation card
- Protected by AuthGuard with ADMIN role requirement

## Error Handling
- Loading spinner during API calls
- Error alert with retry functionality
- Empty state message when no owners found

## Testing
- Unit tests with Jasmine/Karma
- Mocked AdminService for isolated testing
- Tests cover loading, error handling, and data transformation

## Usage
The component is automatically loaded when navigating to the salon owners section from the admin dashboard. It requires admin authentication and will display all pre-seeded salon owners sorted by city.