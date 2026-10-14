// ============================================================
// session.js
// SESSION LAYER — the ONLY file allowed to know "who's logged in".
//
// The backend uses a real HTTP session (a JSESSIONID cookie set by
// POST /api/auth/login). That cookie is HttpOnly, so our JavaScript
// can never read it directly - the only way to know who's logged in
// is to ASK the server: GET /api/profile. A 200 means logged in
// (with the user attached); a 401 means not logged in.
//
// This means checking session state is now a real network call, not
// an instant localStorage read like our old dev version. We cache
// the result in memory (currentUser) so pages don't have to keep
// re-asking the server on every single check within the same page.
// That cache is intentionally NOT persisted (no localStorage) -
// on a fresh page load we always re-ask the server, since only the
// server actually knows if the session cookie is still valid.
// ============================================================

const Session = (() => {
  let currentUser = null; // in-memory cache only; source of truth is the server

  /**
   * Asks the backend who's currently logged in and updates the cache.
   * Call this once per page load (UiHelpers.bootPage() does this for
   * you - see ui-helpers.js - so pages don't need to call it directly
   * in most cases).
   */
  async function refresh() {
    try {
      currentUser = await Api.profile.get();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        currentUser = null; // normal "not logged in" state, not an error
      } else {
        console.error("Session.refresh() failed:", err);
        currentUser = null;
      }
    }
    return currentUser;
  }

  /** Returns the cached user (or null). Sync - reflects the last refresh(). */
  function getCurrentUser() {
    return currentUser;
  }

  function isLoggedIn() {
    return currentUser !== null;
  }

  function getCurrentUserId() {
    return currentUser ? currentUser.userId : null;
  }

  function getCurrentRole() {
    return currentUser ? currentUser.role : null;
  }

  /**
   * Logs in via the real backend and updates the cache from its response.
   * Throws ApiError (e.g. status 401) on bad credentials - callers should
   * catch this and show the message to the user.
   */
  async function login(email, password) {
    currentUser = await Api.auth.login(email, password);
    return currentUser;
  }

  /**
   * Registers a new account. NOTE: this does NOT log the person in -
   * the backend's /api/auth/register does not start a session. Send
   * them to the login page after a successful register.
   */
  function register(data) {
    return Api.auth.register(data);
  }

  /** Logs out via the real backend and clears the cache either way. */
  async function logout() {
    try {
      await Api.auth.logout();
    } finally {
      currentUser = null;
    }
  }

  return {
    refresh,
    getCurrentUser,
    isLoggedIn,
    getCurrentUserId,
    getCurrentRole,
    login,
    register,
    logout,
  };
})();
