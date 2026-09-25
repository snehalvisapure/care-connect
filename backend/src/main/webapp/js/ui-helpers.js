// ============================================================
// ui-helpers.js
// Shared UI utilities. Nothing in this file talks to the network -
// see api.js for that, and session.js for auth state.
// ============================================================

const UiHelpers = (() => {
  /**
   * Shows a small temporary message in the corner of the screen.
   * type: "info" | "success" | "error"
   */
  function toast(message, type = "info") {
    let container = document.getElementById("toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "toast-container";
      document.body.appendChild(container);
    }

    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = message;
    container.appendChild(el);

    requestAnimationFrame(() => el.classList.add("toast-visible"));
    setTimeout(() => {
      el.classList.remove("toast-visible");
      setTimeout(() => el.remove(), 300);
    }, 3500);
  }

  /**
   * Formats a date string/timestamp for display. Falls back to the
   * raw value if it can't be parsed.
   */
  function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (isNaN(date.getTime())) return value;
    return date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  /**
   * Returns a small <span> badge element marking the trust level of a
   * piece of functionality.
   *   "real"       -> a real, verified backend endpoint
   *   "pending"    -> feature can't work yet (backend piece missing)
   *   "unenforced" -> the endpoint is real, but the backend does NOT
   *                   itself check the caller's identity/role for this
   *                   action - it trusts whatever id the client sends.
   *                   Use this on modules other than auth/profile until
   *                   their controllers add their own session checks.
   */
  function badge(kind) {
    const labels = {
      real: "✅ REAL API",
      pending: "⏳ PENDING",
      unenforced: "⚠️ NOT SERVER-ENFORCED",
    };
    const span = document.createElement("span");
    span.className = `status-badge status-badge-${kind}`;
    span.textContent = labels[kind] || kind;
    return span;
  }

  /**
   * Returns a banner element explaining that a feature can't work yet
   * because it depends on a backend piece that isn't built.
   */
  function pendingBanner(featureName) {
    const div = document.createElement("div");
    div.className = "pending-banner";
    div.innerHTML = `
      <strong>⏳ Backend integration pending</strong>
      <p>${featureName} depends on a backend module that isn't built yet.
      This screen is UI-only for now.</p>
    `;
    return div;
  }

  /**
   * Loads an HTML partial (e.g. the shared header/footer) into a
   * target element. Any occurrence of {{ROOT}} inside the partial is
   * replaced with rootPath, so the same partial file works whether
   * it's included from the site root or from a nested pages/ folder.
   *
   * Example, from a page at pages/needs/list.html:
   *   UiHelpers.loadPartial("../../partials/header.html", "#header-slot", "../../");
   */
  async function loadPartial(url, targetSelector, rootPath = "") {
    const target = document.querySelector(targetSelector);
    if (!target) return;
    try {
      const res = await fetch(url);
      let html = await res.text();
      html = html.split("{{ROOT}}").join(rootPath);
      target.innerHTML = html;
    } catch (e) {
      console.error(`Could not load partial: ${url}`, e);
    }
  }

  /**
   * Returns a notice box telling the visitor they need to log in (for
   * real) to use this feature. Distinct from pendingBanner: this means
   * "the API is real, you just aren't logged in" - not "this feature
   * can't work at all yet".
   */
  function loginRequiredNotice(actionDescription, loginPath) {
    const div = document.createElement("div");
    div.className = "auth-required-notice";
    div.innerHTML = `
      <strong>🔒 Log in required</strong>
      <p>${actionDescription} needs you to be logged in.</p>
    `;
    const link = document.createElement("a");
    link.className = "btn btn-primary btn-sm";
    link.href = loginPath;
    link.textContent = "Log in";
    div.appendChild(link);
    return div;
  }

  /**
   * Fills the header's auth slot (#header-auth-slot, injected as part
   * of partials/header.html) based on the CURRENT Session cache. Call
   * this after Session.refresh() has resolved - bootPage() below does
   * this for you.
   */
  function renderAuthSlot(rootPath = "") {
    const slot = document.getElementById("header-auth-slot");
    if (!slot) return;

    const user = Session.getCurrentUser();
    if (!user) {
      slot.innerHTML = `
        <a class="btn btn-outline btn-sm" href="${rootPath}pages/auth/login.html">Log in</a>
        <a class="btn btn-primary btn-sm" href="${rootPath}pages/auth/register.html">Register</a>
      `;
      return;
    }

    // Admin link only shown to ADMIN-role users. This is a client-side
    // convenience only, NOT a security boundary - see the note on
    // Api.admin in api.js. A non-admin who guesses the URL can still
    // open pages/admin/dashboard.html; the page itself re-checks the
    // role and blocks rendering if it doesn't match.
    const adminLink = user.role === "ADMIN"
      ? `<a class="btn btn-outline btn-sm" href="${rootPath}pages/admin/dashboard.html">Admin</a>`
      : "";

    slot.innerHTML = `
      <span class="header-greeting">Hi, ${user.fullName} <span class="text-small">(${user.role})</span></span>
      ${adminLink}
      <a class="btn btn-outline btn-sm" href="${rootPath}pages/auth/profile.html">Profile</a>
      <button type="button" class="btn btn-primary btn-sm" id="header-logout-btn">Log out</button>
    `;
    document.getElementById("header-logout-btn").addEventListener("click", async () => {
      await Session.logout();
      toast("Logged out.", "success");
      window.location.href = `${rootPath}index.html`;
    });
  }

  /**
   * ONE call most pages make at the top of their script. Handles the
   * repetitive setup so individual pages don't have to:
   *   - loads the shared header/footer partials
   *   - calls Session.refresh() (the real /api/profile check) ONCE
   *   - renders the header's login/logout state from the result
   *   - if requireAuth is true and nobody's logged in, redirects to
   *     the login page instead of letting the page render
   *
   * Returns the current user (or null if requireAuth is false and
   * nobody's logged in).
   *
   * Example, from a page at pages/needs/list.html:
   *   const user = await UiHelpers.bootPage({ rootPath: "../../" });
   *
   * Example, from a page that requires login:
   *   const user = await UiHelpers.bootPage({ rootPath: "../../", requireAuth: true });
   *   if (!user) return; // bootPage already redirected - stop here
   */
  async function bootPage({ rootPath = "", requireAuth = false, loginPath = null } = {}) {
    await loadPartial(`${rootPath}partials/header.html`, "#header-slot", rootPath);
    await loadPartial(`${rootPath}partials/footer.html`, "#footer-slot", rootPath);

    const user = await Session.refresh();
    renderAuthSlot(rootPath);

    if (requireAuth && !user) {
      const target = loginPath || `${rootPath}pages/auth/login.html`;
      window.location.href = target;
      return null;
    }

    return user;
  }

  return {
    toast,
    formatDate,
    badge,
    pendingBanner,
    loginRequiredNotice,
    loadPartial,
    renderAuthSlot,
    bootPage,
  };
})();
