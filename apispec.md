# Recruitment Management System API Spec

Source of truth provided by user on 2026-04-27.

## Auth

### POST /auth/signup
~~~json
Body: {"username":"string","email":"string","password":"string"}
Access: public
Behavior: creates account, sends verification code, account starts unverified
Response: 200 OK with saved User entity
~~~

### POST /auth/login
~~~json
Body: {"email":"string","password":"string"}
Access: public
Response: 200 OK
Response body: {"token":"jwt","expiresIn":number}
~~~

### POST /auth/verify
~~~json
Body: {"email":"string","verificationCode":"string"}
Access: public
Response: 200 OK with {"message":"Account verified successfully"}
~~~

### POST /auth/resend?email=user@example.com
~~~json
Body: none
Access: public
Response: 200 OK with {"message":"Verification code sent"}
~~~

### POST /auth/resend-body
~~~json
Body: {"email":"string"}
Access: public
Response: 200 OK with {"message":"Verification code sent"}
~~~

---

## Admin Requests

### POST /admin-requests
~~~json
Body: none
Access: authenticated
Behavior: creates admin request for current user
Response: 201 Created with AdminRequestResponse
~~~

### GET /admin-requests/me
~~~json
Body: none
Access: authenticated
Response: 200 OK with AdminRequestResponse[]
~~~

### GET /admin-requests/pending
~~~json
Body: none
Access: admin only
Response: 200 OK with AdminRequestResponse[]
~~~

### PATCH /admin-requests/{requestId}
~~~json
Body: {"status":"PENDING|APPROVED|REJECTED"}
Access: admin only
Response: 200 OK with AdminRequestResponse
~~~

---

## Facilities (Authenticated User Scope)

### GET /facilities
~~~json
Body: none
Access: authenticated
Query: {"includeInactive":"boolean(optional)","search":"string(optional)"}
Behavior:
- Admin: returns all facilities (includeInactive supported)
- Regular user: returns assigned facilities only
Response: 200 OK with FacilityAccessResponse[]
~~~

### GET /facilities/{facilityId}
~~~json
Body: none
Access: authenticated
Behavior:
- Returns one accessible facility by id
- Regular user: facility must be assigned
Response: 200 OK with FacilityAccessResponse
~~~

### POST /facilities/{facilityId}/email-recipients
~~~json
Body: {"email":"string","name":"string(optional)","isActive":true}
Access: authenticated user with facility access (or admin)
Response: 200 OK with EmailRecipient
~~~

### GET /facilities/{facilityId}/email-recipients
~~~json
Body: none
Access: authenticated user with facility access (or admin)
Query: {"isActive":"boolean(optional)"}
Response: 200 OK with EmailRecipient[]
~~~

### PATCH /facilities/{facilityId}/email-recipients/{recipientId}
~~~json
Body: {"email":"string(optional)","name":"string(optional)","isActive":"boolean(optional)"}
Access: authenticated user with facility access (or admin)
Response: 200 OK with EmailRecipient
~~~

### DELETE /facilities/{facilityId}/email-recipients/{recipientId}
~~~json
Body: none
Access: authenticated user with facility access (or admin)
Response: 200 OK with {"message":"Email recipient removed successfully"}
~~~

---

## Admin Facilities

### POST /admin/facilities/add
~~~json
Body: {"name":"string","location":"string"}
Access: admin only
Response: 200 OK with Facility
~~~

### POST /admin/facilities/{facilityId}/addUser
~~~json
Body: {"email":"string"}
Access: admin only
Response: 200 OK with {"message":"User added to facility successfully"}
~~~

### POST /admin/facilities/{facilityId}/email-recipients/batch
~~~json
Body: {"recipients":[{"email":"string","name":"string(optional)","isActive":true}]}
Access: admin only
Response: 200 OK with EmailRecipient[]
~~~

### GET /admin/facilities
~~~json
Body: none
Access: admin only
Response: 200 OK with FacilitySummaryResponse[]
~~~

### PUT /admin/facilities/{facilityId}
~~~json
Body: {"name":"string","location":"string"}
Access: authenticated (admin or user with facility access)
Response: 200 OK with FacilityDetailResponse
~~~

### DELETE /admin/facilities/{facilityId}
~~~json
Body: none
Access: admin only
Behavior:
- Archives all active applicants in the facility
- Removes user associations from facility
- Deactivates facility email recipients
- Soft-deletes facility by setting active=false and deletedAt
Response: 200 OK with {"message":"Facility archived successfully. Archived applicants: <number>","facilityId":number,"facilityName":"string"}
~~~

### POST /admin/facilities/{facilityId}/recover
~~~json
Body: none
Access: admin only
Behavior:
- Reactivates facility (active=true, deletedAt=null)
- Recovers archived applicants in that facility when no active duplicate email conflicts exist
- Reactivates facility email recipients
Response: 200 OK with FacilityRecoverResponse
Response body: {"message":"Facility recovered successfully","facilityId":number,"facilityName":"string","recoveredApplicants":number}
~~~

### GET /admin/facilities/{facilityId}/users
~~~json
Body: none
Access: admin only
Response: 200 OK with FacilityUserResponse[]
~~~

### DELETE /admin/facilities/{facilityId}/users/{userId}
~~~json
Body: none
Access: admin only
Behavior: removes user from facility (cannot remove last admin user)
Response: 200 OK with {"message":"User removed from facility successfully","facilityId":number,"userId":number,"userEmail":"string"}
~~~

### PATCH /facilities/{facilityId}/email-recipients/{recipientId}/status
~~~
Authorization: Bearer <jwt>
Access: Authenticated users with access to the facility (admin or assigned user)
Content-Type: application/json
~~~

Path params:
~~~json
{
  "facilityId": "number (required)",
  "recipientId": "number (required)"
}
~~~

Request body:
~~~json
{
  "isActive": true
}
~~~

Validation:
- isActive is required (null is rejected with 400 validation error).

Success response:
- Status: 200 OK
- Body type: EmailRecipient
~~~json
{
  "id": 44,
  "facilityId": 12,
  "email": "alerts@northclinic.com",
  "name": "Hiring Alerts",
  "isActive": false
}
~~~

---

## Applicants

### POST /applicants
~~~json
Body: multipart/form-data {
  "name":"string",
  "email":"string",
  "phoneNumber":"string(optional)",
  "role":"string",
  "facilityId":number,
  "status":"ApplicantStatus(optional)",
  "notes":"string(optional)",
  "resume":"file(required)"
}
Access: authenticated with facility access (or admin)
Response: 201 Created with ApplicantResponse
~~~

### GET /applicants
~~~json
Body: none
Access: authenticated
Query: {
  "facilityId":"number(optional)",
  "status":"ApplicantStatus(optional)",
  "role":"string(optional)",
  "fromDate":"yyyy-MM-dd(optional)",
  "toDate":"yyyy-MM-dd(optional)",
  "page":"number(default 0)",
  "size":"number(default 20)",
  "sort":"string(default addedDate,desc)"
}
Behavior: returns active (non-archived) applicants only
Response: 200 OK with Page<ApplicantResponse>
~~~

### GET /applicants/{applicantId}
~~~json
Body: none
Access: authenticated with facility access (or admin)
Response: 200 OK with ApplicantResponse
~~~

### PATCH /applicants/{applicantId}/status
~~~json
Body: {"status":"ApplicantStatus","notes":"string(optional)"}
Access: authenticated with facility access (or admin)
Response: 200 OK with ApplicantResponse
~~~

### PUT /applicants/{applicantId}
~~~json
Body: multipart/form-data {
  "name":"string(optional)",
  "email":"string(optional)",
  "phoneNumber":"string(optional)",
  "role":"string(optional)",
  "notes":"string(optional)",
  "resume":"file(optional)"
}
Access: authenticated with facility access (or admin)
Response: 200 OK with ApplicantResponse
~~~

### GET /applicants/{applicantId}/resume
~~~json
Body: none
Access: authenticated with facility access (or admin)
Response: 200 OK with file stream
~~~

### POST /applicants/{applicantId}/interviews
~~~json
Body: {
  "scheduledDate":"ISO datetime",
  "durationMinutes":60,
  "interviewType":"string",
  "notes":"string(optional)",
  "generateMeetingLink":true,
  "sendCalendarInvites":true,
  "sendEmailNotifications":true
}
Access: authenticated with facility access (or admin)
Response: 201 Created with ApplicantInterviewResponse
Interview type must be one of: phone, video, in-person
~~~

### PATCH /applicants/{applicantId}/interviews/{interviewId}/complete
~~~json
Body: {
  "completed":true,
  "noShow":false,
  "notes":"string(optional)",
  "newStatus":"ApplicantStatus(optional)"
}
Rules: completed and noShow are mutually exclusive (cannot both be true).
Access: authenticated with facility access (or admin)
Response: 200 OK with ApplicantInterviewResponse
~~~

### POST /applicants/{applicantId}/orientation
~~~json
Body: {
  "scheduledDate":"ISO datetime",
  "durationMinutes":120,
  "documentsRequired":["string(optional)"],
  "generateMeetingLink":true,
  "sendEmailNotifications":true
}
Access: authenticated with facility access (or admin)
Response: 201 Created with ApplicantOrientationResponse
~~~

### PATCH /applicants/{applicantId}/orientation/{orientationId}/complete
~~~json
Body: {"completed":true,"noShow":false,"notes":"string(optional)"}
Access: authenticated with facility access (or admin)
Response: 200 OK with ApplicantOrientationResponse
~~~

### PATCH /applicants/bulk/status
~~~json
Body: {"applicantIds":[1,2,3],"status":"ApplicantStatus","notes":"string(optional)"}
Access: authenticated
Response: 200 OK with BulkStatusUpdateResponse
~~~

### GET /applicants/stats
~~~json
Body: none
Access: authenticated
Query: {"facilityId":"number(optional)","fromDate":"yyyy-MM-dd(optional)","toDate":"yyyy-MM-dd(optional)"}
Response: 200 OK with ApplicantStatsResponse
~~~

### GET /applicants/search?q=term
~~~json
Body: none
Access: authenticated
Query: {
  "q":"string(required)",
  "facilityId":"number(optional)",
  "status":"ApplicantStatus(optional)",
  "limit":"number(default 20)"
}
Behavior: searches active (non-archived) applicants only
Response: 200 OK with ApplicantResponse[]
~~~

### POST /applicants/{applicantId}/resend-notification
~~~json
Body: {"notificationType":"INTERVIEW_SCHEDULED|ORIENTATION_SCHEDULED|HIRED|STATUS_UPDATE"}
Access: authenticated with facility access (or admin)
Response: 200 OK with NotificationResponse
~~~

### DELETE /applicants/{applicantId}
~~~json
Body: none
Access: authenticated with facility access (or admin)
Behavior: archives applicant (soft delete)
Response: 200 OK with DeletedApplicantResponse
Response body: {"applicantId":number,"archived":true,"archivedAt":"ISO datetime","message":"Applicant archived successfully"}
~~~

### DELETE /applicants/bulk
~~~json
Body: {"applicantIds":[1,2,3]}
Access: authenticated with facility access (or admin)
Behavior: archives multiple applicants (soft delete)
Response: 200 OK with BulkDeletedApplicantResponse
Response body: {"archivedCount":number,"archivedIds":[number],"failedIds":[number],"message":"string"}
~~~

### POST /applicants/{applicantId}/recover
~~~json
Body: none
Access: authenticated with facility access (or admin)
Behavior:
- Recovers one archived applicant
- Fails if facility is inactive
- Fails if an active applicant with same email exists in the facility
Response: 200 OK with RecoveredApplicantResponse
Response body: {"applicantId":number,"archived":false,"recoveredAt":"ISO datetime","message":"Applicant recovered successfully"}
~~~

### POST /applicants/bulk/recover
~~~json
Body: {"applicantIds":[1,2,3]}
Access: authenticated with facility access (or admin)
Behavior: recovers archived applicants in bulk; skips conflicted items
Response: 200 OK with BulkRecoveredApplicantResponse
Response body: {"recoveredCount":number,"recoveredIds":[number],"failedIds":[number],"message":"string"}
~~~

## Calendar

### GET /calendar/interviews
~~~json
Body: none
Access: authenticated
Query: {
  "facilityId":"number(optional)",
  "startDate":"ISO datetime(required)",
  "endDate":"ISO datetime(required)",
  "status":"string(optional)",
  "interviewType":"string(optional)",
  "view":"month|week|day|agenda(default month)"
}
Response: 200 OK with CalendarEventsResponse
~~~

### GET /calendar/interviews/upcoming
~~~json
Body: none
Access: authenticated
Query: {"facilityId":"number(optional)","days":"number(default 7)","limit":"number(default 50)"}
Response: 200 OK with UpcomingInterviewsResponse
~~~

### GET /calendar/interviews/{interviewId}
~~~json
Body: none
Access: authenticated with facility access (or admin)
Response: 200 OK with CalendarEventDto
~~~

### GET /calendar/orientations
~~~json
Body: none
Access: authenticated
Query: {
  "facilityId":"number(optional)",
  "startDate":"ISO datetime(required)",
  "endDate":"ISO datetime(required)",
  "status":"string(optional)",
  "view":"month|week|day|agenda(default month)"
}
Response: 200 OK with CalendarEventsResponse
~~~

### GET /calendar/orientations/upcoming
~~~json
Body: none
Access: authenticated
Query: {"facilityId":"number(optional)","days":"number(default 7)","limit":"number(default 50)"}
Response: 200 OK with UpcomingOrientationsResponse
~~~

### GET /calendar/orientations/{orientationId}
~~~json
Body: none
Access: authenticated with facility access (or admin)
Response: 200 OK with CalendarEventDto
~~~

### GET /calendar/export
~~~json
Body: none
Access: authenticated
Query: {
  "facilityId":"number(optional)",
  "startDate":"yyyy-MM-dd(required)",
  "endDate":"yyyy-MM-dd(required)",
  "format":"ics|csv|json(default ics)"
}
Response: 200 OK with downloadable file bytes
~~~

### POST /calendar/sync-external
~~~json
Body: {"facilityId":number,"calendarType":"string","accessToken":"string","refreshToken":"string(optional)"}
Access: admin only
Response: 200 OK with ExternalSyncResponse
~~~

### POST /calendar/meeting-link/{interviewId}/regenerate
~~~json
Body: {"sendUpdateNotifications":true} (optional body)
Access: authenticated with facility access (or admin)
Response: 200 OK with RegenerateMeetingLinkResponse
~~~

### POST /calendar/reminders/send-now/{interviewId}
~~~json
Body: {"reminderType":"24h|1h"}
Access: authenticated with facility access (or admin)
Response: 200 OK with ReminderSentResponse
~~~

### POST /calendar/availability/check
~~~json
Body: {"facilityId":number,"startDateTime":"ISO datetime","endDateTime":"ISO datetime"}
Access: authenticated with facility access (or admin)
Response: 200 OK with AvailabilityResponse
~~~
