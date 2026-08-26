// ============================================================
// api.js
// THE ONLY FILE ALLOWED TO CALL fetch() AGAINST THE BACKEND.
//
// Every page calls functions like Api.needs.list(institutionId)
// instead of writing fetch() itself. This means:
//   - If the backend base URL changes, we fix it in ONE place.
//   - Every function here maps to a REAL, verified endpoint from the
//     backend controllers - nothing in this file is invented.
//
// AUTH NOTE: login uses a real HTTP session (a JSESSIONID cookie),
// not a token we attach ourselves. apiRequest() below always sends
// credentials: "include" so that cookie rides along automatically.
//
// ⚠️ This currently WILL NOT WORK across localhost:8000 (frontend)
// and localhost:8080 (backend) until Member 1 adds a CORS filter
// that allows credentialed cross-origin requests. See the CORS notes
// this project's plan - that change is Member 1's, not made here.
//
// Module namespaces (institutions, needs, donations, ...) are filled
// in only when we build that specific module (Steps 3-9), against
// endpoints already verified directly from the backend source code.
// ============================================================

const API_BASE_URL = "http://localhost:8080/careconnect/api";
// ^ Adjust this ONE line once the backend is actually deployed.
//   "careconnect" is a guess at the Tomcat context path based on the
//   project name - confirm the real path with Member 1 and update here.

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = "ApiError";
    this.status = status;   // HTTP status code, or 0 for network failure
    this.body = body;       // parsed error JSON from the backend, if any
  }
}

/**
 * Internal fetch wrapper. Not exported - every real call goes through
 * the module namespaces below instead of calling this directly from a page.
 */
async function apiRequest(path, options = {}) {
  const url = `${API_BASE_URL}${path}`;
  const config = {
    headers: { "Content-Type": "application/json" },
    credentials: "include", // sends/receives the session cookie (JSESSIONID)
    ...options,
  };

  let response;
  try {
    response = await fetch(url, config);
  } catch (networkError) {
    throw new ApiError(
      "Could not reach the backend server. Is it running?",
      0,
      null
    );
  }

  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (e) {
      body = null; // non-JSON response body
    }
  }

  if (!response.ok) {
    const message =
      body && body.error ? body.error : `Request failed (${response.status})`;
    throw new ApiError(message, response.status, body);
  }

  return body;
}

const Api = {
  // ------------------------------------------------------------
  // Auth — REAL endpoints (AuthController.java). Session-based:
  // login/logout don't return a token - the browser gets a session
  // cookie automatically via credentials: "include" above.
  // ------------------------------------------------------------
  auth: {
    // POST /api/auth/register
    // body: { fullName, email, password, phone, role, address }
    //   role must be "DONOR" | "VOLUNTEER" | "INSTITUTION" (ADMIN is
    //   blocked server-side - see UserService.register).
    // -> 201 Created, returns the new User (no session started yet -
    //    the person still has to log in afterward).
    // -> 400 { error: "..." } on invalid input, e.g. duplicate email.
    register(data) {
      return apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // POST /api/auth/login   body: { email, password }
    // -> 200, returns the User, and the backend sets the session cookie.
    // -> 401 { error: "Invalid email or password" } on bad credentials.
    login(email, password) {
      return apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
    },
    // POST /api/auth/logout (no body)
    // -> 200 { message: "Logged out successfully" }; invalidates the session.
    logout() {
      return apiRequest("/auth/logout", { method: "POST" });
    },
  },

  // ------------------------------------------------------------
  // Profile — REAL endpoints (ProfileController.java). All three
  // require an active session; each returns 401 { error: "Not logged
  // in." } if there isn't one - Session.refresh() relies on this.
  // ------------------------------------------------------------
  profile: {
    // GET /api/profile -> 200, current logged-in User.
    get() {
      return apiRequest("/profile");
    },
    // PUT /api/profile   body: { fullName, phone, address, profilePhoto }
    // Does NOT change email/password/role. -> 200, updated User.
    update(data) {
      return apiRequest("/profile", {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },
    // POST /api/profile/password   body: { oldPassword, newPassword }
    // -> 200 { message: "Password changed successfully" }
    // -> 400 { error: "Current password is incorrect" } etc.
    changePassword(oldPassword, newPassword) {
      return apiRequest("/profile/password", {
        method: "POST",
        body: JSON.stringify({ oldPassword, newPassword }),
      });
    },
  },

  // ------------------------------------------------------------
  // Institutions — REAL, verified endpoints (InstitutionController.java)
  // ------------------------------------------------------------
  institutions: {
    // GET /api/institutions or /api/institutions?approvedOnly=true
    list(approvedOnly = false) {
      const query = approvedOnly ? "?approvedOnly=true" : "";
      return apiRequest(`/institutions${query}`);
    },
    // GET /api/institutions?userId={id} -> single Institution or null
    getByUserId(userId) {
      return apiRequest(`/institutions?userId=${userId}`);
    },
    // GET /api/institutions/{id}
    getById(id) {
      return apiRequest(`/institutions/${id}`);
    },
    // GET /api/institutions/{id}/dashboard
    getDashboard(id) {
      return apiRequest(`/institutions/${id}/dashboard`);
    },
    // POST /api/institutions
    // body: { userId, institutionName, institutionType, registrationNumber,
    //         description, address, city, state, pincode, contactPerson }
    register(data) {
      return apiRequest("/institutions", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // PUT /api/institutions/{id}
    update(id, data) {
      return apiRequest(`/institutions/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },
    // POST /api/institutions/{id}/approve
    approve(id) {
      return apiRequest(`/institutions/${id}/approve`, { method: "POST" });
    },
    // POST /api/institutions/{id}/reject   body: { rejectionReason }
    reject(id, rejectionReason) {
      return apiRequest(`/institutions/${id}/reject`, {
        method: "POST",
        body: JSON.stringify({ rejectionReason }),
      });
    },
    // DELETE /api/institutions/{id}
    remove(id) {
      return apiRequest(`/institutions/${id}`, { method: "DELETE" });
    },
  },

  // ------------------------------------------------------------
  // Needs — REAL, verified endpoints (NeedController.java).
  // NOTE: listing REQUIRES institutionId - there is no "all needs
  // across every institution" endpoint here. That capability lives
  // on Api.search.needs() instead (Step 8), not here.
  // ------------------------------------------------------------
  needs: {
    // GET /api/needs?institutionId={id}
    listByInstitution(institutionId) {
      return apiRequest(`/needs?institutionId=${institutionId}`);
    },
    // GET /api/needs/{id}
    getById(id) {
      return apiRequest(`/needs/${id}`);
    },
    // POST /api/needs
    // body: { institutionId, category, itemName, quantityRequired, urgency, description }
    // Starts as status=OPEN, quantityReceived=0 - not settable here.
    create(data) {
      return apiRequest("/needs", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // PUT /api/needs/{id}
    // body: { category, itemName, quantityRequired, urgency, description }
    // quantityReceived/status are NOT editable here - they only change
    // automatically when a donation against this need is delivered.
    update(id, data) {
      return apiRequest(`/needs/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },
    // DELETE /api/needs/{id}
    remove(id) {
      return apiRequest(`/needs/${id}`, { method: "DELETE" });
    },
  },

  // ------------------------------------------------------------
  // Donations (monetary) — REAL, verified endpoints (DonationController.java).
  // There's no separate "pledge" resource in the schema - a pledge IS a
  // Donation row with paymentStatus = "PENDING". confirm() moves it to
  // COMPLETED (with a transactionId + auto-generated receiptNumber);
  // cancel() only works while it's still PENDING.
  // No session/ownership check on the backend - see the "unenforced"
  // badge used on every donations page.
  // ------------------------------------------------------------
  donations: {
    // GET /api/donations?donorId={id} -> full donation history for a donor
    listByDonor(donorId) {
      return apiRequest(`/donations?donorId=${donorId}`);
    },
    // GET /api/donations?institutionId={id} -> full donation history for an institution
    listByInstitution(institutionId) {
      return apiRequest(`/donations?institutionId=${institutionId}`);
    },
    // GET /api/donations/pending?donorId={id} -> a donor's pending pledges only
    listPendingByDonor(donorId) {
      return apiRequest(`/donations/pending?donorId=${donorId}`);
    },
    // GET /api/donations/pending?institutionId={id} -> an institution's pending pledges only
    listPendingByInstitution(institutionId) {
      return apiRequest(`/donations/pending?institutionId=${institutionId}`);
    },
    // GET /api/donations/{id}
    getById(id) {
      return apiRequest(`/donations/${id}`);
    },
    // POST /api/donations   body: { donorId, institutionId, needId (optional), amount }
    // -> 201 Created, a new Donation with donationType="MONEY",
    //    paymentStatus="PENDING", donationStatus="PENDING".
    create(data) {
      return apiRequest("/donations", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // POST /api/donations/{id}/confirm   body: { transactionId }
    // No real payment gateway exists - this is how we simulate "the
    // payment actually went through". -> paymentStatus/donationStatus
    // both move to COMPLETED, receiptNumber gets auto-generated.
    confirm(id, transactionId) {
      return apiRequest(`/donations/${id}/confirm`, {
        method: "POST",
        body: JSON.stringify({ transactionId }),
      });
    },
    // POST /api/donations/{id}/cancel (no body)
    // Only works while paymentStatus is still PENDING.
    cancel(id) {
      return apiRequest(`/donations/${id}/cancel`, { method: "POST" });
    },
  },

  // ------------------------------------------------------------
  // Item donations (in-kind) — REAL, verified endpoints
  // (ItemDonationController.java). Every item donation is backed by a
  // parent Donation row (donationType="ITEM", amount=0.00) created
  // automatically by the backend - this API only returns the
  // item-specific fields (itemName, quantity, pickupOrDrop,
  // deliveryStatus...), not donorId/institutionId directly. Use
  // Api.donations.getById(itemDonation.donationId) if you need those.
  // ------------------------------------------------------------
  itemDonations: {
    // GET /api/item-donations?donorId={id}
    listByDonor(donorId) {
      return apiRequest(`/item-donations?donorId=${donorId}`);
    },
    // GET /api/item-donations?institutionId={id}
    listByInstitution(institutionId) {
      return apiRequest(`/item-donations?institutionId=${institutionId}`);
    },
    // GET /api/item-donations/{id}
    getById(id) {
      return apiRequest(`/item-donations/${id}`);
    },
    // POST /api/item-donations
    // body: { donorId, institutionId, needId (optional), itemName,
    //         quantity, pickupOrDrop ("PICKUP"|"DROP_OFF"), pickupAddress }
    //   pickupAddress is required only when pickupOrDrop is "PICKUP".
    // -> 201 Created, deliveryStatus starts at "PENDING".
    create(data) {
      return apiRequest("/item-donations", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // POST /api/item-donations/{id}/pickup (no body)
    // PENDING -> PICKED_UP. Only meaningful for pickupOrDrop="PICKUP";
    // DROP_OFF donations skip straight from PENDING to DELIVERED.
    markPickedUp(id) {
      return apiRequest(`/item-donations/${id}/pickup`, { method: "POST" });
    },
    // POST /api/item-donations/{id}/deliver   body: { deliveryDate (optional, "yyyy-MM-dd") }
    // -> DELIVERED. This is the point where, if a needId was attached,
    //    the linked need's quantityReceived actually advances.
    markDelivered(id, deliveryDate = null) {
      return apiRequest(`/item-donations/${id}/deliver`, {
        method: "POST",
        body: JSON.stringify(deliveryDate ? { deliveryDate } : {}),
      });
    },
    // POST /api/item-donations/{id}/cancel (no body)
    // Blocked once the donation is already DELIVERED.
    cancel(id) {
      return apiRequest(`/item-donations/${id}/cancel`, { method: "POST" });
    },
  },
  // ------------------------------------------------------------
  // Volunteers — REAL endpoints (VolunteerController.java), but LIMITED.
  // The service layer has getProfileByUserId()/updateProfile()/
  // deleteProfile() but the controller never wires them up - only
  // list-all, get-by-id, and create exist over HTTP. So there's no way
  // to ask the backend "does user X already have a volunteer profile"
  // directly; we list all and filter client-side. Editing/deleting a
  // profile isn't possible from this frontend at all right now (⏳
  // PENDING - not a gap in this frontend, a gap in the controller).
  // ------------------------------------------------------------
  volunteers: {
    // GET /api/volunteers -> every volunteer profile in the system
    list() {
      return apiRequest("/volunteers");
    },
    // GET /api/volunteers/{id}
    getById(id) {
      return apiRequest(`/volunteers/${id}`);
    },
    // POST /api/volunteers   body: { userId, skills, availability, experience }
    // -> 201 Created. -> 409 if this userId already has a profile.
    create(data) {
      return apiRequest("/volunteers", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // NOTE: no update()/remove() here on purpose - the controller
    // doesn't expose PUT/DELETE for volunteer profiles even though
    // VolunteerService supports it internally.
  },

  // ------------------------------------------------------------
  // Volunteer opportunities — REAL endpoints
  // (VolunteerOpportunityController.java). Posted by institutions,
  // applied to by volunteers via Api.registrations below.
  // ------------------------------------------------------------
  opportunities: {
    // GET /api/opportunities -> public browsing, OPEN status only
    listOpen() {
      return apiRequest("/opportunities");
    },
    // GET /api/opportunities?institutionId={id} -> ALL statuses for that institution
    listByInstitution(institutionId) {
      return apiRequest(`/opportunities?institutionId=${institutionId}`);
    },
    // GET /api/opportunities/{id}
    getById(id) {
      return apiRequest(`/opportunities/${id}`);
    },
    // POST /api/opportunities
    // body: { institutionId, title, description, requiredSkills,
    //         eventDate "yyyy-MM-dd", startTime "HH:mm:ss" (optional),
    //         endTime "HH:mm:ss" (optional), location, maxVolunteers (optional) }
    // -> 201, status starts at "OPEN".
    create(data) {
      return apiRequest("/opportunities", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // POST /api/opportunities/{id}/close (no body) -> status -> CLOSED
    close(id) {
      return apiRequest(`/opportunities/${id}/close`, { method: "POST" });
    },
    // POST /api/opportunities/{id}/complete (no body) -> status -> COMPLETED
    complete(id) {
      return apiRequest(`/opportunities/${id}/complete`, { method: "POST" });
    },
  },

  // ------------------------------------------------------------
  // Volunteer registrations (applications) — REAL endpoints
  // (VolunteerRegistrationController.java). Links a Volunteer to a
  // VolunteerOpportunity - one application per volunteer per
  // opportunity (DB-enforced UNIQUE constraint).
  // ------------------------------------------------------------
  registrations: {
    // GET /api/registrations?opportunityId={id} -> every applicant for one opportunity
    listByOpportunity(opportunityId) {
      return apiRequest(`/registrations?opportunityId=${opportunityId}`);
    },
    // GET /api/registrations?volunteerId={id} -> every application a volunteer has made
    listByVolunteer(volunteerId) {
      return apiRequest(`/registrations?volunteerId=${volunteerId}`);
    },
    // POST /api/registrations   body: { volunteerId, opportunityId }
    // -> 201, status starts at "APPLIED".
    // -> 409 if the opportunity isn't OPEN, or already applied to it.
    apply(volunteerId, opportunityId) {
      return apiRequest("/registrations", {
        method: "POST",
        body: JSON.stringify({ volunteerId, opportunityId }),
      });
    },
    // POST /api/registrations/{id}/accept (no body)
    // -> 409 if the opportunity's maxVolunteers cap is already full.
    accept(id) {
      return apiRequest(`/registrations/${id}/accept`, { method: "POST" });
    },
    // POST /api/registrations/{id}/reject (no body)
    reject(id) {
      return apiRequest(`/registrations/${id}/reject`, { method: "POST" });
    },
    // POST /api/registrations/{id}/complete (no body)
    complete(id) {
      return apiRequest(`/registrations/${id}/complete`, { method: "POST" });
    },
    // DELETE /api/registrations/{id} -> withdraw an application
    withdraw(id) {
      return apiRequest(`/registrations/${id}`, { method: "DELETE" });
    },
  },
  // ------------------------------------------------------------
  // Events — REAL endpoints (EventController.java). Full CRUD, unlike
  // VolunteerOpportunity - events have no OPEN/CLOSED lifecycle, they
  // just exist on a date and users RSVP to them.
  // ------------------------------------------------------------
  events: {
    // GET /api/events -> public browsing, upcoming events only
    listUpcoming() {
      return apiRequest("/events");
    },
    // GET /api/events?category=FESTIVAL
    listByCategory(category) {
      return apiRequest(`/events?category=${encodeURIComponent(category)}`);
    },
    // GET /api/events?institutionId={id} -> ALL events (any date) for that institution
    listByInstitution(institutionId) {
      return apiRequest(`/events?institutionId=${institutionId}`);
    },
    // GET /api/events/{id}
    getById(id) {
      return apiRequest(`/events/${id}`);
    },
    // POST /api/events
    // body: { institutionId, title, description, eventDate "yyyy-MM-dd",
    //         eventTime "HH:mm:ss" (optional), location, category }
    create(data) {
      return apiRequest("/events", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // PUT /api/events/{id}   same body shape as create (institutionId not changeable)
    update(id, data) {
      return apiRequest(`/events/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
      });
    },
    // DELETE /api/events/{id}
    remove(id) {
      return apiRequest(`/events/${id}`, { method: "DELETE" });
    },
  },

  // ------------------------------------------------------------
  // Event RSVPs — REAL endpoints (EventRsvpController.java). This is
  // an upsert design: POST always either creates or updates the
  // single RSVP row for that (eventId, userId) pair - there's no
  // separate "change my RSVP" endpoint, you just POST again.
  // ------------------------------------------------------------
  rsvps: {
    // GET /api/rsvps?eventId={id} -> everyone's RSVP for one event
    listByEvent(eventId) {
      return apiRequest(`/rsvps?eventId=${eventId}`);
    },
    // GET /api/rsvps?userId={id} -> a user's RSVPs across all events
    listByUser(userId) {
      return apiRequest(`/rsvps?userId=${userId}`);
    },
    // GET /api/rsvps?eventId={id}&userId={id} -> this user's status for
    // this event, or null if they haven't RSVP'd at all.
    getMyStatus(eventId, userId) {
      return apiRequest(`/rsvps?eventId=${eventId}&userId=${userId}`);
    },
    // GET /api/rsvps/count?eventId={id} -> { going: N }
    countGoing(eventId) {
      return apiRequest(`/rsvps/count?eventId=${eventId}`);
    },
    // POST /api/rsvps   body: { eventId, userId, response }
    // Upsert - creates the RSVP if none exists yet, otherwise updates it.
    setRsvp(eventId, userId, response) {
      return apiRequest("/rsvps", {
        method: "POST",
        body: JSON.stringify({ eventId, userId, response }),
      });
    },
    // DELETE /api/rsvps?eventId={id}&userId={id} -> remove the RSVP entirely
    remove(eventId, userId) {
      return apiRequest(`/rsvps?eventId=${eventId}&userId=${userId}`, { method: "DELETE" });
    },
  },
  // ------------------------------------------------------------
  // Search & Filter — REAL endpoints (SearchController.java, Module 7).
  // Read-only, dynamic-filter search across institutions and needs.
  // Every filter param is optional - omit/pass null to skip it.
  //
  // Institutions: the backend ALWAYS forces verificationStatus=APPROVED
  // for public search, no matter what you pass, unless you also pass
  // includeUnapproved: true (e.g. for an admin view in Step 9). There is
  // no "public" way to filter by a specific verificationStatus - if
  // includeUnapproved is false, the status filter is ignored entirely.
  //
  // Needs: NeedSearchDAO does NOT join against institutions at all, so
  // results can include needs belonging to a PENDING/REJECTED
  // institution - filtering by institution approval isn't something
  // this endpoint does. Flagged in the UI.
  // ------------------------------------------------------------
  search: {
    // GET /api/search/institutions?city=&type=&verificationStatus=&includeUnapproved=
    institutions({ city, type, verificationStatus, includeUnapproved = false } = {}) {
      const params = new URLSearchParams();
      if (city) params.set("city", city);
      if (type) params.set("type", type);
      if (includeUnapproved && verificationStatus) params.set("verificationStatus", verificationStatus);
      if (includeUnapproved) params.set("includeUnapproved", "true");
      const query = params.toString();
      return apiRequest(`/search/institutions${query ? `?${query}` : ""}`);
    },
    // GET /api/search/needs?category=&urgency=&institutionId=&status=
    needs({ category, urgency, institutionId, status } = {}) {
      const params = new URLSearchParams();
      if (category) params.set("category", category);
      if (urgency) params.set("urgency", urgency);
      if (institutionId) params.set("institutionId", institutionId);
      if (status) params.set("status", status);
      const query = params.toString();
      return apiRequest(`/search/needs${query ? `?${query}` : ""}`);
    },
  },
  // ------------------------------------------------------------
  // Admin — REAL endpoints (AdminController.java, Module 9), but
  // read-only and NARROW: only "view all users" and "view all
  // donations" exist here. There is no dedicated admin endpoint for
  // approving/rejecting institutions - that real functionality
  // already lives on Api.institutions.approve()/reject() (built back
  // in Step 3), so the Admin page calls those directly instead of
  // duplicating them here.
  //
  // ⚠️ NOT SERVER-ENFORCED: AdminController does not check the
  // caller's session or role at all - same pattern as every other
  // module. The Admin page below only gates access client-side by
  // checking Session.getCurrentRole() === "ADMIN"; nothing stops a
  // non-admin from calling these URLs directly (e.g. via curl).
  //
  // Also note: /api/admin/donations only queries the `donations`
  // table (monetary donations) - it does NOT include item donations,
  // since AdminDonationDAO has no equivalent query against that table.
  // ------------------------------------------------------------
  admin: {
    // GET /api/admin/users or /api/admin/users?role=DONOR|VOLUNTEER|INSTITUTION|ADMIN
    listUsers(role) {
      const query = role ? `?role=${encodeURIComponent(role)}` : "";
      return apiRequest(`/admin/users${query}`);
    },
    // GET /api/admin/donations -> every monetary donation in the system, newest first
    listDonations() {
      return apiRequest("/admin/donations");
    },
  },

  // ------------------------------------------------------------
  // Institution verification documents — REAL endpoints
  // (InstitutionDocumentController.java). Note: documentPath is a
  // plain string field - the backend does not implement real file
  // storage/upload, so this expects a URL/path string (e.g. a
  // Google Drive link), not a binary file.
  // ------------------------------------------------------------
  documents: {
    // GET /api/documents?institutionId={id}
    listByInstitution(institutionId) {
      return apiRequest(`/documents?institutionId=${institutionId}`);
    },
    // GET /api/documents/{id}
    getById(id) {
      return apiRequest(`/documents/${id}`);
    },
    // POST /api/documents   body: { institutionId, documentType, documentPath }
    upload(data) {
      return apiRequest("/documents", {
        method: "POST",
        body: JSON.stringify(data),
      });
    },
    // DELETE /api/documents/{id}?institutionId={id}
    remove(id, institutionId) {
      return apiRequest(`/documents/${id}?institutionId=${institutionId}`, {
        method: "DELETE",
      });
    },
  },
};
