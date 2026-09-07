import type { NextResponse } from "next/server";

export const deviceCookieName = "deadlines_device_id";

export function newDeviceId() {
  return crypto.randomUUID();
}

export function setDeviceCookie(response: NextResponse, deviceId: string) {
  response.cookies.set(deviceCookieName, deviceId, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 365,
  });
}
