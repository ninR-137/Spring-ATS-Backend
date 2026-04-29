- **Blocked static assets:** Security used literal paths (`"/css/"`, `"/js/"`) so requests like `/css/styles.css` were blocked.  
  **Fix:** Permit patterns with wildcards in SecurityConfiguration.java: `"/css/**", "/js/**", "/images/**", "/favicon.ico"`.
- **Root redirect caused a resource lookup:** Root (`/`) redirected to `/landing` (no trailing slash) but the controller handled `/landing/`, so Spring tried to find a static resource named `landing` and threw `NoResourceFoundException`. That forwarded to `/error` and produced a 403 because `/error` was protected.  
  **Fixes:** 
  - Updated root redirect to `redirect:/landing/` in RootController.java.  
  - Made the landing controller handle both `/landing` and `/landing/` in LandingController.java.
- **Error endpoint blocked:** The error dispatch (`/error`) itself was protected, causing a 403 instead of showing the real exception.  
  **Fix:** Added `"/error"` to the permit list in `SecurityConfiguration`.
- **H2 console blocked in a frame:** Browser refused to show H2 because of X-Frame-Options.  
  **Fix:** Allowed framing for the console in `SecurityConfiguration` (we used header config to permit frames for H2; consider using `sameOrigin()` instead of disabling for safer behavior).
- **Chrome DevTools probe caused 403:** DevTools requested `/.well-known/...`, which was rejected and triggered the error flow.  
  **Fix:** Permitted `"/.well-known/**"` in security matchers.
- **Diagnosis steps that helped:** Enabled `spring.security.debug=true` and `server.error.include-stacktrace=always` in application.properties and watched the boot logs to see which requests and filters caused rejections.

Files changed (high level)
- Security rules: SecurityConfiguration.java  
- Root redirect: RootController.java  
- Landing controller: LandingController.java  
- App properties (debug/stacktrace): application.properties

Would you like me to:
- switch frame-options to `sameOrigin()` (safer), and
- remove `spring.security.debug=true` (and the stacktrace setting) now that it's fixed?