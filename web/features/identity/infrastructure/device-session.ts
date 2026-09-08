import type { NextResponse } from "next/server";
import { authCookies, persistentCookieOptions } from "@/lib/cookies";

export const deviceCookieName = authCookies.deviceId;

export function newDeviceId() {
  return crypto.randomUUID();
}

export function setDeviceCookie(response: NextResponse, deviceId: string) {
  response.cookies.set(deviceCookieName, deviceId, persistentCookieOptions(60 * 60 * 24 * 365));
}
