# NavbarComponent

A role-aware navigation component for the Salon Management Application.

## Features

- **Role-based Navigation**: Shows different navigation links based on the current user's role (CUSTOMER, SALON_OWNER, PROFESSIONAL, ADMIN)
- **Responsive Design**: Uses Bootstrap navbar with mobile-friendly collapsible menu
- **Theme Integration**: Styled with the app's primary color (#244AFD) and white text
- **Logout Functionality**: Includes a logout button that calls AuthService.logout() and redirects to login page
- **Auto-hide**: Only displays when user is logged in

## Usage

The NavbarComponent is automatically included in the app layout via the SharedModule. It will:

1. Check the current authentication state on initialization
2. Display appropriate navigation links based on the user's role
3. Show/hide based on login status
4. Handle logout functionality

## Navigation Links by Role

### Customer
- Browse Professionals
- My Appointments  
- My Profile

### Salon Owner
- Dashboard
- Staff Management
- Services
- Appointments

### Professional
- Dashboard
- My Profile

### Admin
- Dashboard
- User Management
- Salon Owners
- Reports

## Styling

The component uses Bootstrap classes with custom SCSS for:
- Primary color (#244AFD) background
- White text and hover effects
- Responsive behavior
- Dropdown styling for user menu

## Integration

The component is already integrated into the main app layout and will automatically appear on all authenticated pages. No additional setup is required.