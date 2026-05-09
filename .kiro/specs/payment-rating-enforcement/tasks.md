# Implementation Plan: Payment & Rating Enforcement

## Overview

Implement the full payment-and-rating lifecycle for GlamBook appointments across the MySQL schema, Spring Boot backend, and Angular frontend. Tasks are ordered so each step builds on the previous one: schema first, then backend entities/repositories, then service logic, then controllers, then the scheduler, then the frontend, and finally tests wired throughout.

## Tasks

- [x] 1. Apply database schema migrations
  - Add `appointment_id BIGINT NULL` FK column to `Reviews` table with `ON DELETE SET NULL` constraint referencing `Appointments(id)`
  - Add `reminder_count INT NOT NULL DEFAULT 0` and `last_reminder_sent_at DATETIME NULL` columns to `Appointments` table
  - Create `admin_notifications` table with columns: `id BIGINT AUTO_INCREMENT PK`, `message TEXT NOT NULL`, `reference_id BIGINT NULL`, `is_read BOOLEAN NOT NULL DEFAULT FALSE`, `created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
  - Append all three DDL statements to `backend/src/main/resources/alter.sql`
  - _Requirements: 7.4, 8.1_

- [x] 2. Extend enums and add `DuplicateReviewException`
  - [x] 2.1 Add `PAYMENT_REMINDER` to `CustomerNotificationType` enum in `backend/src/main/java/com/salon/entity/CustomerNotificationType.java`
    - _Requirements: 7.6_
  - [x] 2.2 Add `UNPAID_APPOINTMENT_ESCALATION` to `NotificationType` enum in `backend/src/main/java/com/salon/entity/NotificationType.java`
    - _Requirements: 7.7_
  - [x] 2.3 Create `DuplicateReviewException` in `backend/src/main/java/com/salon/exception/DuplicateReviewException.java` extending `RuntimeException`
    - Register it in `GlobalExceptionHandler` to return HTTP 409 with message body
    - _Requirements: 8.3_

- [x] 3. Update JPA entities
  - [x] 3.1 Add `appointment` FK field to `Review` entity
    - Add `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id") private Appointment appointment;` to `backend/src/main/java/com/salon/entity/Review.java`
    - _Requirements: 8.1, 8.2_
  - [x] 3.2 Add reminder-tracking fields to `Appointment` entity
    - Add `@Column(name = "reminder_count") @Builder.Default private int reminderCount = 0;` and `@Column(name = "last_reminder_sent_at") private LocalDateTime lastReminderSentAt;` to `backend/src/main/java/com/salon/entity/Appointment.java`
    - _Requirements: 6.6_
  - [x] 3.3 Create `AdminNotification` entity at `backend/src/main/java/com/salon/entity/AdminNotification.java`
    - Fields: `id`, `message` (TEXT), `referenceId` (appointmentId), `isRead` (default false), `createdAt` (`@CreationTimestamp`)
    - Use `@Table(name = "admin_notifications")`
    - _Requirements: 7.4_

- [x] 4. Add repository methods
  - [x] 4.1 Add to `ReviewRepository`:
    - `boolean existsByCustomerIdAndAppointmentId(Long customerId, Long appointmentId)` — used by `PaymentService` rating check
    - `boolean existsByAppointmentId(Long appointmentId)` — used by `ReviewController` exists endpoint
    - Remove or replace the existing workaround JPQL `findByAppointmentId` query with the direct FK-based derived method now that the `appointment` FK column exists
    - _Requirements: 4.1, 8.4_
  - [x] 4.2 Add to `AppointmentRepository`:
    - `@Query("SELECT a FROM Appointment a WHERE a.status = 'CONFIRMED' AND a.dateTime < :now AND NOT EXISTS (SELECT p FROM Payment p WHERE p.appointment = a AND p.status = 'PAID')") List<Appointment> findOverdueUnpaid(@Param("now") LocalDateTime now);`
    - _Requirements: 6.1_
  - [x] 4.3 Add to `CustomerNotificationRepository`:
    - `long countByCustomerIdAndReferenceIdAndType(Long customerId, Long referenceId, CustomerNotificationType type);`
    - _Requirements: 6.2_
  - [x] 4.4 Create `AdminNotificationRepository` at `backend/src/main/java/com/salon/repository/AdminNotificationRepository.java`
    - Extends `JpaRepository<AdminNotification, Long>`
    - Add `boolean existsByReferenceId(Long referenceId);`
    - Add `List<AdminNotification> findAllByOrderByCreatedAtDesc();`
    - _Requirements: 7.1, 7.3, 7.5_

- [x] 5. Update `ReviewRequest` DTO and `ReviewService`
  - [x] 5.1 Add `appointmentId` field to `ReviewRequest`
    - Add `@NotNull private Long appointmentId;` to `backend/src/main/java/com/salon/dto/request/ReviewRequest.java`
    - Add `@Size(max = 1000, message = "Comment must not exceed 1000 characters") private String comment;` constraint (replace bare `String comment`)
    - _Requirements: 3.6, 8.2_
  - [x] 5.2 Update `ReviewService.createReview` to enforce appointment-scoped uniqueness and persist the FK
    - Resolve `Appointment` from `appointmentRepository.findById(request.getAppointmentId())` — throw `ResourceNotFoundException` if absent
    - Call `reviewRepository.existsByCustomerIdAndAppointmentId(customerId, appointmentId)` — throw `DuplicateReviewException("A review for this appointment already exists")` if true
    - Set `review.setAppointment(appointment)` before saving
    - After saving, build and persist a `ProfessionalNotification` with `type = NEW_REVIEW`, `referenceId = saved.getId()`, `message = "{customerName} rated you {rating}/5: {comment.substring(0, min(100, comment.length()))}"`
    - Inject `AppointmentRepository` and `ProfessionalNotificationRepository` into `ReviewService`
    - _Requirements: 5.1, 5.2, 5.3, 8.2, 8.3_
  - [ ]* 5.3 Write unit tests for updated `ReviewService` in `backend/src/test/java/com/salon/review/ReviewServiceTest.java`
    - Test: happy path — review saved with correct `appointment.id`, `ProfessionalNotification` of type `NEW_REVIEW` created with correct `referenceId`
    - Test: duplicate review for same `(customerId, appointmentId)` throws `DuplicateReviewException`
    - Test: notification message contains customer name, rating, and first 100 chars of comment
    - _Requirements: 5.1, 5.2, 5.3, 8.2, 8.3_

- [x] 6. Update `PaymentService` and `PaymentController` with timing and rating guards
  - [x] 6.1 Modify `PaymentService.processPayment` to accept `customerId` as a second parameter
    - Insert timing check before the duplicate-payment check: `if (appointment.getDateTime().isAfter(LocalDateTime.now())) throw new ValidationException("Payment is not allowed before the appointment time");`
    - Insert rating check after timing check: `if (!reviewRepository.existsByCustomerIdAndAppointmentId(customerId, appointmentId)) throw new ValidationException("A rating must be submitted before payment can be processed");` — use HTTP 422 by creating a new `UnprocessableEntityException` or map `ValidationException` with a distinct subtype; register in `GlobalExceptionHandler` to return 422
    - Inject `ReviewRepository` into `PaymentService`
    - _Requirements: 2.1, 2.2, 4.1, 4.2, 4.3_
  - [x] 6.2 Update `PaymentController.processPayment` to extract `customerId` from JWT and pass it to `PaymentService`
    - Inject `JwtUtil`, extract `customerId` from `Authorization` header
    - Verify `appointment.getCustomer().getId().equals(customerId)` — throw `AccessDeniedException` (HTTP 403) if not
    - Pass `customerId` to `paymentService.processPayment(request, customerId)`
    - _Requirements: 2.3_
  - [ ]* 6.3 Write unit tests for updated `PaymentService` in `backend/src/test/java/com/salon/payment/PaymentServiceTest.java`
    - Test: future `dateTime` → `ValidationException` with message `"Payment is not allowed before the appointment time"`
    - Test: past `dateTime` + no review → `ValidationException` with message `"A rating must be submitted before payment can be processed"`
    - Test: past `dateTime` + review exists → payment created successfully
    - Test: appointment not found → `ResourceNotFoundException`
    - _Requirements: 2.1, 2.2, 4.1, 4.2_
  - [ ]* 6.4 Write property-based tests for `PaymentService` in `backend/src/test/java/com/salon/payment/PaymentServicePropertyTest.java` using jqwik
    - **Property 4: Backend rejects payment for future appointments** — generate arbitrary `LocalDateTime` values strictly after `now`; assert `processPayment` throws `ValidationException` with the timing message
    - **Validates: Requirements 2.1**
    - **Property 5: Backend accepts payment for past/present appointments (timing check only)** — generate arbitrary `LocalDateTime` values at or before `now`; assert no timing `ValidationException` is thrown (mock review to exist)
    - **Validates: Requirements 2.2**
    - **Property 9: Backend rejects payment when no review exists** — generate arbitrary past `dateTime` values with `reviewRepository.existsByCustomerIdAndAppointmentId` returning `false`; assert `ValidationException` with the rating message
    - **Validates: Requirements 4.1, 4.2**

- [ ] 7. Checkpoint — ensure all backend service tests pass
  - Run `mvn test -pl backend -Dtest="PaymentServiceTest,PaymentServicePropertyTest,ReviewServiceTest"` and confirm green; ask the user if questions arise.

- [x] 8. Update `ReviewController` and add `ReviewService.existsByAppointmentId`
  - [x] 8.1 Add `existsByAppointmentId(Long appointmentId)` method to `ReviewService` that delegates to `reviewRepository.existsByAppointmentId(appointmentId)`
    - _Requirements: 3.1_
  - [x] 8.2 Add `GET /api/reviews/exists?appointmentId={id}` endpoint to `ReviewController`
    - `@GetMapping("/exists") @PreAuthorize("hasRole('CUSTOMER')") public ResponseEntity<Map<String,Boolean>> exists(@RequestParam Long appointmentId)`
    - Returns `ResponseEntity.ok(Map.of("exists", reviewService.existsByAppointmentId(appointmentId)))`
    - _Requirements: 3.1, 3.7_
  - [ ]* 8.3 Write unit tests for the new endpoint in `backend/src/test/java/com/salon/review/ReviewControllerTest.java`
    - Test: `GET /api/reviews/exists?appointmentId=1` returns `{"exists": true}` when review exists
    - Test: returns `{"exists": false}` when no review exists
    - _Requirements: 3.1_

- [x] 9. Create `AdminNotification` service and controller
  - [x] 9.1 Create `AdminNotificationService` at `backend/src/main/java/com/salon/service/AdminNotificationService.java` (interface) and `impl/AdminNotificationServiceImpl.java`
    - `List<AdminNotificationResponse> getAll()` — calls `adminNotificationRepository.findAllByOrderByCreatedAtDesc()`, maps to `AdminNotificationResponse`
    - `void markAsRead(Long id)` — loads entity, sets `isRead = true`, saves
    - _Requirements: 7.5_
  - [x] 9.2 Create `AdminNotificationResponse` DTO at `backend/src/main/java/com/salon/dto/response/AdminNotificationResponse.java`
    - Fields: `id`, `message`, `referenceId`, `isRead`, `createdAt`
    - _Requirements: 7.4, 7.5_
  - [x] 9.3 Create `AdminNotificationController` at `backend/src/main/java/com/salon/controller/AdminNotificationController.java`
    - `GET /api/admin/notifications` → `@PreAuthorize("hasRole('ADMIN')")` → returns `List<AdminNotificationResponse>` ordered by `createdAt DESC`
    - `PATCH /api/admin/notifications/{id}/read` → marks as read, returns 200
    - _Requirements: 7.5_
  - [ ]* 9.4 Write unit tests for `AdminNotificationService` in `backend/src/test/java/com/salon/admin/AdminNotificationServiceTest.java`
    - Test: `getAll()` returns list ordered by `createdAt` descending
    - Test: `markAsRead` sets `isRead = true`
    - _Requirements: 7.5_

- [x] 10. Implement `ReminderScheduler`
  - [x] 10.1 Create `ReminderScheduler` at `backend/src/main/java/com/salon/scheduler/ReminderScheduler.java`
    - Annotate with `@Component`, enable scheduling via `@EnableScheduling` on `AppConfig` (or `SalonManagementApplication`)
    - `@Scheduled(fixedRate = 3_600_000)` on `processOverdueAppointments()`
    - Query `appointmentRepository.findOverdueUnpaid(LocalDateTime.now())`
    - For each appointment: if `reminderCount < 3` AND (`lastReminderSentAt == null` OR `lastReminderSentAt.isBefore(now.minusHours(24))`): create `CustomerNotification(PAYMENT_REMINDER)` with `referenceId = appt.id`, message = `"Reminder: Your appointment for {serviceName} with {professionalName} on {dateTime} is unpaid. Please complete your payment to avoid further action."`, increment `reminderCount`, set `lastReminderSentAt = now`, save appointment
    - Else if `reminderCount >= 3` AND `!adminNotificationRepository.existsByReferenceId(appt.id)`: create `AdminNotification` with message = `"Customer {customerName} (ID: {customerId}) has not paid after 3 reminders for {serviceName} with {professionalName} on {dateTime}. Please review and take action."`, `referenceId = appt.id`
    - Wrap each appointment's processing in try/catch, log ERROR and continue on failure
    - Inject `AppointmentRepository`, `CustomerNotificationRepository`, `AdminNotificationRepository`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6, 7.1, 7.2, 7.3_
  - [ ]* 10.2 Write unit tests for `ReminderScheduler` in `backend/src/test/java/com/salon/scheduler/ReminderSchedulerTest.java`
    - Test: appointment with `reminderCount = 0` and no recent reminder → one `PAYMENT_REMINDER` created, `reminderCount` incremented to 1
    - Test: appointment with `lastReminderSentAt` within last 24 hours → no new notification created
    - Test: appointment with `reminderCount = 3` and no existing `AdminNotification` → one `AdminNotification` created
    - Test: appointment with `reminderCount = 3` and existing `AdminNotification` → no duplicate created
    - _Requirements: 6.2, 6.6, 7.1, 7.3_
  - [ ]* 10.3 Write property-based tests for `ReminderScheduler` in `backend/src/test/java/com/salon/scheduler/ReminderSchedulerPropertyTest.java` using jqwik
    - **Property 15: Overdue appointment with fewer than 3 reminders receives a PAYMENT_REMINDER** — generate appointments with `reminderCount` in `[0, 2]` and `lastReminderSentAt` older than 24 h or null; assert exactly one `PAYMENT_REMINDER` created and `reminderCount` incremented by 1
    - **Validates: Requirements 6.2**
    - **Property 16: Reminder message contains required appointment fields** — generate arbitrary service names, professional names, and dateTimes; assert generated message contains all three
    - **Validates: Requirements 6.3, 6.4**
    - **Property 17: No duplicate reminder within 24-hour window** — generate appointments with `lastReminderSentAt` within the last 24 hours; assert no new `PAYMENT_REMINDER` is created
    - **Validates: Requirements 6.6**
    - **Property 18: Escalation fires exactly once per appointment** — generate appointments with `reminderCount == 3` and no existing `AdminNotification`; run scheduler twice; assert exactly one `AdminNotification` exists after both runs
    - **Validates: Requirements 7.1, 7.3**
    - **Property 19: Escalation message contains required fields** — generate arbitrary customer names, customer IDs, service names, professional names, and dateTimes; assert `AdminNotification` message contains all five
    - **Validates: Requirements 7.2**
  - [ ]* 10.4 Write property-based tests for `ReviewService` notification in `backend/src/test/java/com/salon/review/ReviewServicePropertyTest.java` using jqwik
    - **Property 10: Review creation triggers NEW_REVIEW notification** — generate arbitrary `(customerId, professionalId, rating[1-5], comment)`; assert a `ProfessionalNotification` of type `NEW_REVIEW` exists with `referenceId == savedReview.id`
    - **Validates: Requirements 5.1, 5.3**
    - **Property 11: Review notification message contains required fields** — generate arbitrary customer names, ratings in `[1,5]`, and comments; assert message contains name, rating string, and `comment.substring(0, min(100, comment.length()))`
    - **Validates: Requirements 5.2**
    - **Property 13: Review persisted with appointment FK** — generate arbitrary valid `appointmentId` values; assert `savedReview.getAppointment().getId() == appointmentId`
    - **Validates: Requirements 8.2**
    - **Property 14: Duplicate review per appointment is rejected** — generate arbitrary `(customerId, appointmentId)` pairs; assert second `createReview` call throws `DuplicateReviewException`
    - **Validates: Requirements 8.3**

- [ ] 11. Checkpoint — ensure all backend tests pass
  - Run `mvn test -pl backend` and confirm green; ask the user if questions arise.

- [x] 12. Update Angular frontend — `CustomerAppointmentsComponent` logic
  - [x] 12.1 Add new state fields and lifecycle hooks to `customer-appointments.component.ts`
    - Add `reviewedAppointments: Map<number, boolean> = new Map();`
    - Add `pendingPayAppt: AppointmentResponse | null = null;`
    - Add `private payTimerHandle: ReturnType<typeof setInterval> | null = null;`
    - Implement `ngOnDestroy()` to clear the interval
    - Call `startPayTimer()` from `ngOnInit()`
    - `startPayTimer()` sets a 60-second `setInterval` that calls `this.cdr.markForCheck()` (inject `ChangeDetectorRef`)
    - _Requirements: 1.3_
  - [x] 12.2 Implement `isPayButtonEnabled(appt: AppointmentResponse): boolean`
    - Returns `Date.now() >= new Date(appt.scheduledAt).getTime()`
    - _Requirements: 1.1, 1.2_
  - [x] 12.3 Implement `payButtonTooltip(appt: AppointmentResponse): string`
    - Returns `"Payment available from " + new Date(appt.scheduledAt).toLocaleString()` when `!isPayButtonEnabled(appt)`, empty string otherwise
    - _Requirements: 1.4_
  - [x] 12.4 Implement `openPayGuarded(appt: AppointmentResponse): void`
    - If `reviewedAppointments.get(appt.id)` is true → call existing `openPay(appt)`
    - Else call `checkReviewExists(appt.id)` then: if exists → cache true, call `openPay(appt)`; else → set `pendingPayAppt = appt`, call `openReview(appt)`
    - _Requirements: 3.1, 3.2, 3.7_
  - [x] 12.5 Implement `onRatingSubmitted(): void`
    - Called at the end of the successful `submitReview()` HTTP response handler (after `reviewSuccess` is set)
    - Caches `reviewedAppointments.set(pendingPayAppt.id, true)`, calls `openPay(pendingPayAppt)`, clears `pendingPayAppt`
    - _Requirements: 3.3, 3.5_
  - [x] 12.6 Implement `checkReviewExists(apptId: number): void`
    - Calls `GET /api/reviews/exists?appointmentId={apptId}` via `HttpClient`
    - On success: caches result in `reviewedAppointments`
    - _Requirements: 3.1_
  - [x] 12.7 Update `submitReview()` to include `appointmentId` in the POST body
    - Change body to `{ professionalId: ..., appointmentId: ..., rating: ..., comment: ... }`
    - On success, call `onRatingSubmitted()` instead of just `closeReview()`
    - _Requirements: 3.3, 8.2_
  - [x] 12.8 Update `closeReview()` to clear `pendingPayAppt` when the modal is dismissed without submitting
    - Set `pendingPayAppt = null` inside `closeReview()`
    - _Requirements: 3.4_
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.7_

- [x] 13. Update Angular frontend — HTML template
  - [x] 13.1 Replace the existing `(click)="openPay(a)"` binding on the "Pay & Complete" button with `(click)="openPayGuarded(a)"`
    - Add `[disabled]="!isPayButtonEnabled(a)"` binding
    - Add `[title]="payButtonTooltip(a)"` binding for the tooltip
    - _Requirements: 1.1, 1.2, 1.4, 3.1_
  - [x] 13.2 Add `maxlength="1000"` attribute to the review comment `<textarea>` in the Rating Modal
    - Add character counter `<small>{{ reviewComment.length }}/1000</small>` below the textarea
    - _Requirements: 3.6_
  - _Requirements: 1.1, 1.2, 1.4, 3.6_

- [x] 14. Add `ReviewService.existsByAppointmentId` to Angular `ReviewService`
  - Add `existsByAppointment(appointmentId: number): Observable<{ exists: boolean }>` method to `frontend/src/app/services/review.service.ts`
  - Calls `GET /api/reviews/exists?appointmentId={appointmentId}`
  - Update `checkReviewExists` in the component to use this service method
  - _Requirements: 3.1_

- [ ] 15. Write Angular property-based and unit tests
  - [ ]* 15.1 Write property-based tests for `isPayButtonEnabled` and `payButtonTooltip` using fast-check in `frontend/src/app/features/customer/customer-appointments/customer-appointments.component.spec.ts`
    - **Property 1 & 2: Pay button enabled/disabled based on scheduledAt** — generate arbitrary ISO date strings; assert `isPayButtonEnabled` returns `true` iff `Date.now() >= new Date(scheduledAt).getTime()`
    - **Validates: Requirements 1.1, 1.2**
    - **Property 3: Tooltip contains scheduled time** — generate arbitrary future ISO date strings; assert `payButtonTooltip` return value contains the formatted `scheduledAt`
    - **Validates: Requirements 1.4**
    - **Property 7: Valid rating enables payment flow** — generate arbitrary integers in `[1, 5]`; set `reviewRating` to each; assert `submitReview()` success path calls `onRatingSubmitted()` and opens payment modal
    - **Validates: Requirements 3.3, 3.5**
    - **Property 8: Comment length validation** — generate arbitrary strings; assert `reviewComment.length <= 1000` is enforced by the `maxlength` binding (test via form control or direct length check)
    - **Validates: Requirements 3.6**
  - [ ]* 15.2 Write unit tests for `openPayGuarded` routing logic
    - Test: `reviewedAppointments` has `true` for appointment → `openPay` called directly, no HTTP call
    - Test: `reviewedAppointments` has no entry, API returns `{ exists: true }` → `openPay` called
    - Test: `reviewedAppointments` has no entry, API returns `{ exists: false }` → `openReview` called, `pendingPayAppt` set
    - Test: `closeReview()` without submit → `pendingPayAppt` is null, `openPay` not called
    - _Requirements: 3.1, 3.2, 3.4, 3.7_
  - [ ]* 15.3 Write property-based tests for notification ordering in `frontend/src/app/features/customer/customer-appointments/customer-appointments.component.spec.ts`
    - **Property 12: Notifications ordered by createdAt descending** — generate arbitrary arrays of notification objects with random `createdAt` values; sort them as the service would; assert every consecutive pair satisfies `n[i].createdAt >= n[i+1].createdAt`
    - **Validates: Requirements 5.4, 7.5**

- [ ] 16. Final checkpoint — ensure all tests pass
  - Run `mvn test -pl backend` (backend) and `ng test --include="**/customer-appointments*" --watch=false` (frontend); confirm green; ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints at tasks 7, 11, and 16 ensure incremental validation
- Property tests validate universal correctness properties; unit tests validate specific examples and edge cases
- The `DuplicateReviewException` (HTTP 409) follows the existing `ConflictException` pattern already registered in `GlobalExceptionHandler`
- The rating-before-payment HTTP 422 response requires either a new `UnprocessableEntityException` class or mapping `ValidationException` by message prefix — choose whichever is consistent with the team's exception strategy
- `@EnableScheduling` must be present on one `@Configuration` class; add it to `AppConfig` if not already present
