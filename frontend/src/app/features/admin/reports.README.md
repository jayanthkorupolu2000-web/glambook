# ReportsComponent

## Overview
The ReportsComponent displays summary statistics for the salon management system, showing total appointments and payments in an attractive dashboard-style layout.

## Features
- **Summary Cards**: Displays total appointments and payments in Bootstrap card layout
- **Loading States**: Shows spinner while fetching data
- **Error Handling**: Displays error messages with retry functionality
- **Refresh Capability**: Manual refresh button to reload data
- **Responsive Design**: Mobile-friendly layout with proper Bootstrap grid
- **Theme Integration**: Uses the app's primary color (#244AFD) for consistent styling

## API Integration
- Calls `GET /api/admin/reports` endpoint via AdminService
- Expects response format: `{ totalAppointments: number, totalPayments: number }`
- Handles loading states and error scenarios gracefully

## Routing
- Accessible at `/admin/reports`
- Protected by admin role authentication
- Integrated into the admin module routing

## Styling
- Uses Bootstrap 5 classes for layout and components
- Custom SCSS for hover effects and theme colors
- Responsive design with mobile-first approach
- Icon integration with Font Awesome

## Testing
- Unit tests cover component initialization, data loading, error handling
- Tests verify proper template rendering and user interactions
- Mocked AdminService for isolated testing

## Usage
The component is automatically loaded when navigating to the reports section from the admin dashboard. It provides a clean overview of system statistics for administrators.