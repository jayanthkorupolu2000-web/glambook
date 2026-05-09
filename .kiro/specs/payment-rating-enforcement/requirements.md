# Requirements Document

## Introduction

This feature enforces a strict payment and rating lifecycle for GlamBook appointments. It ensures that:
- Customers can only pay once the appointment time has arrived or passed (no pre-payment).
- Customers must submit a star rating and feedback before the payment modal opens.
- Submitted reviews are delivered as notifications to the professional and appear in their reviews section.
- Customers who have confirmed appointments with overdue unpaid balances are reminded automatically; repeated non-payment is escalated to the Admin.

The feature spans the Angular 16 frontend (UI enforcement), Spring Boot 3 backend (business rule validation, scheduled jobs), and MySQL database (new columns and notification types).

---

## Glossary

- **Appointment**: A confirmed booking between a Customer and a Professional for a Service, stored in the `Appointments` table with a `dateTime` field representing the scheduled start time.
- **Customer**: A registered platform user with the CUSTOMER role who books appointments.
- **Professional**: A registered platform user with the PROFESSIONAL role who delivers services.
- **Admin**: The fixed-credential superuser who oversees the platform and handles escalations.
- **Payment**: A record in the `Payments` table linked one-to-one with an Appointment, tracking amount, method, and status.
- **Review**: A record in the `Reviews` table containing a 1–5 star rating and optional text comment, linked to a Customer and a Professional.
- **Pay_Button**: The "Pay & Complete" button rendered in the customer appointments UI for CONFIRMED appointments.
- **Rating_Modal**: The Angular modal component that collects a star rating (1–5) and feedback text before payment.
- **Payment_Modal**: The Angular modal component that collects payment method and card details to process payment.
- **Payment_Guard**: The frontend logic that blocks the Payment_Modal from opening until a rating has been submitted for the appointment.
- **Overdue_Appointment**: A CONFIRMED appointment whose `dateTime` has passed and whose linked Payment record has status `PENDING` or does not exist.
- **Payment_Reminder**: A `CustomerNotification` of type `PAYMENT_REMINDER` sent to a Customer for an Overdue_Appointment.
- **Escalation_Notification**: An admin-level notification created when a Customer has received 3 Payment_Reminders for the same appointment without paying.
- **Reminder_Scheduler**: The Spring `@Scheduled` component that detects Overdue_Appointments and dispatches Payment_Reminders or Escalation_Notifications.
- **AdminNotification**: A notification entity targeting the Admin, stored in a dedicated table, used for escalation alerts.

---

## Requirements

### Requirement 1: Payment Timing Enforcement (Frontend)

**User Story:** As a customer, I want the "Pay & Complete" button to be disabled until the appointment time has arrived, so that I cannot accidentally pay before the service begins.

#### Acceptance Criteria

1. WHILE the current client-side timestamp is before `appointment.scheduledAt`, THE Pay_Button SHALL be rendered in a disabled state and SHALL NOT open the Payment_Modal on click.
2. WHEN the current client-side timestamp is greater than or equal to `appointment.scheduledAt`, THE Pay_Button SHALL be rendered in an enabled state.
3. THE Pay_Button SHALL re-evaluate its enabled/disabled state every 60 seconds while the appointments page is open, so that it becomes enabled without requiring a page reload.
4. WHEN a Customer attempts to open the Payment_Modal for an appointment whose `scheduledAt` is in the future, THE Pay_Button SHALL display a tooltip or inline message stating "Payment available from [scheduledAt time]".

---

### Requirement 2: Payment Timing Enforcement (Backend)

**User Story:** As a platform operator, I want the backend to reject payment requests submitted before the appointment time, so that the business rule is enforced even if the frontend check is bypassed.

#### Acceptance Criteria

1. WHEN `POST /api/payments` is called with an `appointmentId` whose `dateTime` is in the future relative to the server's current time, THE Payment_Controller SHALL reject the request with HTTP 400 and an error message of "Payment is not allowed before the appointment time".
2. WHEN `POST /api/payments` is called with an `appointmentId` whose `dateTime` is in the past or equal to the server's current time, THE Payment_Controller SHALL process the payment normally.
3. IF the appointment referenced by `appointmentId` does not exist or does not belong to the authenticated Customer, THEN THE Payment_Controller SHALL return HTTP 404 or HTTP 403 respectively.

---

### Requirement 3: Mandatory Rating Before Payment (Frontend)

**User Story:** As a platform operator, I want customers to rate their professional before paying, so that feedback is always collected for completed services.

#### Acceptance Criteria

1. WHEN a Customer clicks the Pay_Button for a CONFIRMED appointment, THE Payment_Guard SHALL check whether a Review already exists for that appointment's professional submitted by that Customer for that appointment.
2. IF no Review exists for the appointment, THEN THE Payment_Guard SHALL open the Rating_Modal instead of the Payment_Modal.
3. WHEN the Customer submits the Rating_Modal with a rating between 1 and 5 (inclusive), THE Payment_Guard SHALL close the Rating_Modal and open the Payment_Modal.
4. IF the Customer closes the Rating_Modal without submitting a rating, THEN THE Payment_Guard SHALL keep the Payment_Modal closed and SHALL NOT proceed to payment.
5. THE Rating_Modal SHALL require a numeric star rating between 1 and 5 inclusive before the submit button is enabled.
6. THE Rating_Modal SHALL accept an optional text feedback field of up to 1000 characters.
7. WHEN a Review already exists for the appointment (re-attempt after prior rating), THE Payment_Guard SHALL open the Payment_Modal directly without showing the Rating_Modal again.

---

### Requirement 4: Mandatory Rating Before Payment (Backend)

**User Story:** As a platform operator, I want the backend to verify that a rating has been submitted before processing payment, so that the rule is enforced server-side.

#### Acceptance Criteria

1. WHEN `POST /api/payments` is called for an `appointmentId`, THE Payment_Controller SHALL verify that a Review record exists linking the authenticated Customer to the Professional of that appointment.
2. IF no such Review record exists, THEN THE Payment_Controller SHALL reject the request with HTTP 422 and an error message of "A rating must be submitted before payment can be processed".
3. WHEN both the timing check (Requirement 2) and the rating check pass, THE Payment_Controller SHALL proceed to create the Payment record.

---

### Requirement 5: Review Notification to Professional

**User Story:** As a professional, I want to be notified when a customer submits a review for me, so that I can read the feedback promptly.

#### Acceptance Criteria

1. WHEN `POST /api/reviews` successfully creates a Review record, THE Review_Service SHALL create a ProfessionalNotification of type `NEW_REVIEW` for the reviewed Professional.
2. THE ProfessionalNotification message SHALL include the Customer's name, the star rating, and the first 100 characters of the feedback comment (if provided).
3. THE ProfessionalNotification SHALL set `referenceId` to the ID of the newly created Review record.
4. WHEN a Professional fetches `GET /api/professional/notifications`, THE Notification_Controller SHALL include the `NEW_REVIEW` notification in the response list ordered by `createdAt` descending.
5. THE Review record SHALL be retrievable via the existing `GET /api/reviews?professionalId={id}` endpoint so that it appears in the Professional's reviews/feedback section.

---

### Requirement 6: Unpaid Appointment Reminder Notifications

**User Story:** As a customer, I want to receive a reminder notification when I have an unpaid confirmed appointment that has already passed, so that I remember to complete my payment.

#### Acceptance Criteria

1. THE Reminder_Scheduler SHALL run every 60 minutes and query for all Overdue_Appointments (CONFIRMED status, `dateTime` < current server time, no COMPLETED Payment record linked).
2. WHEN an Overdue_Appointment is found and the Customer has received fewer than 3 Payment_Reminders for that appointment, THE Reminder_Scheduler SHALL create a CustomerNotification of type `PAYMENT_REMINDER` for that Customer.
3. THE Payment_Reminder message SHALL include the service name, professional name, appointment date/time, and the text "Please complete your payment to avoid further action".
4. THE CustomerNotification SHALL set `referenceId` to the appointment ID.
5. WHEN a Customer fetches their notifications, THE Customer_Notification_Controller SHALL include `PAYMENT_REMINDER` notifications in the response.
6. THE Reminder_Scheduler SHALL NOT send a duplicate Payment_Reminder to the same Customer for the same appointment within a 24-hour window.

---

### Requirement 7: Admin Escalation After 3 Unpaid Reminders

**User Story:** As an admin, I want to be notified when a customer has ignored 3 payment reminders for the same appointment, so that I can take appropriate action.

#### Acceptance Criteria

1. WHEN the Reminder_Scheduler detects that a Customer has already received exactly 3 Payment_Reminders for a specific Overdue_Appointment and the appointment is still unpaid, THE Reminder_Scheduler SHALL create an AdminNotification for the Admin.
2. THE AdminNotification message SHALL include the Customer's name, Customer ID, service name, professional name, appointment date/time, and the text "Customer has not paid after 3 reminders. Please review and take action."
3. THE Reminder_Scheduler SHALL NOT create more than one AdminNotification per appointment (i.e., escalation fires exactly once per appointment, not on every subsequent scheduler run).
4. THE AdminNotification SHALL be stored in a dedicated `admin_notifications` table with fields: id, message, referenceId (appointmentId), isRead, createdAt.
5. WHEN the Admin fetches `GET /api/admin/notifications`, THE Admin_Notification_Controller SHALL return all AdminNotifications ordered by `createdAt` descending.
6. THE `CustomerNotificationType` enum SHALL include the value `PAYMENT_REMINDER` to support Payment_Reminder notifications.
7. THE `NotificationType` enum (used for AdminNotification) SHALL include the value `UNPAID_APPOINTMENT_ESCALATION` to support escalation notifications.

---

### Requirement 8: Appointment-Scoped Review Linking

**User Story:** As a platform operator, I want reviews to be linked to a specific appointment, so that the rating-before-payment check can be performed accurately per appointment.

#### Acceptance Criteria

1. THE Review entity SHALL include an `appointment` field (ManyToOne relationship to Appointment) to associate each review with the specific appointment it was submitted for.
2. WHEN `POST /api/reviews` is called, THE Review_Controller SHALL accept an `appointmentId` field in the request body and persist the Review with the linked Appointment.
3. THE Review_Service SHALL enforce that only one Review per Customer per Appointment exists; IF a duplicate is submitted, THEN THE Review_Service SHALL return HTTP 409 with the message "A review for this appointment already exists".
4. WHEN the Payment_Controller checks for a pre-payment review, THE Payment_Controller SHALL query for a Review by `customerId` AND `appointmentId` (not just `professionalId`) to ensure appointment-level accuracy.
