export const authCookies = {
  accessToken: "opensources_access_token",
  refreshToken: "opensources_refresh_token",
  lastActivity: "opensources_last_activity",
  persistentSession: "opensources_persistent_session",
  deviceId: "opensources_device_id",
} as const;

export const preferenceCookies = {
  locale: "opensources_locale",
  theme: "opensources_theme",
  timezone: "opensources_timezone",
} as const;

export const invitationCookie = "opensources_invitation_token";
export const csrfCookie = "opensources_csrf_token";

const isProduction = process.env.NODE_ENV === "production";

export const secureCookieOptions = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: isProduction,
  path: "/",
};

export const csrfCookieOptions = {
  httpOnly: false,
  sameSite: "strict" as const,
  secure: isProduction,
  path: "/",
};

export function persistentCookieOptions(maxAge: number) {
  return { ...secureCookieOptions, maxAge };
}

export const legacyCookies = [
  "deadlines_access_token",
  "deadlines_refresh_token",
  "deadlines_last_activity",
  "deadlines_persistent_session",
  "deadlines_device_id",
  "deadlines_locale",
  "deadlines_theme",
  "deadlines_timezone",
  "deadlines_invitation_token",
] as const;

export function clearLegacyCookies(response: { cookies: { delete(name: string): unknown } }) {
  for (const cookieName of legacyCookies) response.cookies.delete(cookieName);
}
