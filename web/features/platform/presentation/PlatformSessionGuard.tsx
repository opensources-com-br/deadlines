"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

const refreshIntervalMs = 14 * 60 * 1000;
const inactivityTimeoutMs = 15 * 60 * 1000;
const activitySyncIntervalMs = 60 * 1000;

export function PlatformSessionGuard({ persistentSession }: { persistentSession: boolean }) {
  const router = useRouter();

  useEffect(() => {
    function revalidateRestoredPage(event: PageTransitionEvent) {
      const navigation = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
      if (event.persisted || navigation?.type === "back_forward") window.location.reload();
    }

    window.addEventListener("pageshow", revalidateRestoredPage);
    const handleExpiredSession = () => window.location.replace("/login");
    window.addEventListener("api:session-expired", handleExpiredSession);

    let refreshing = false;
    let lastActivityAt = Date.now();
    let lastRefreshAt = Date.now();
    let lastActivitySyncAt = 0;

    function signOutForInactivity() {
      void fetch("/api/auth/logout", { method: "POST", cache: "no-store" }).finally(() => window.location.replace("/login"));
    }

    function recordActivity() {
      lastActivityAt = Date.now();
      if (persistentSession && lastActivityAt - lastRefreshAt >= refreshIntervalMs) {
        void refreshSession();
        return;
      }
      if (lastActivityAt - lastActivitySyncAt < activitySyncIntervalMs) return;
      lastActivitySyncAt = lastActivityAt;
      void fetch("/api/auth/activity", { method: "POST", cache: "no-store" }).then((response) => {
        if (response.status === 401) window.location.replace("/login");
      });
    }

    async function refreshSession() {
      if (refreshing) return;
      if (Date.now() - lastActivityAt >= inactivityTimeoutMs) {
        if (!persistentSession) signOutForInactivity();
        return;
      }
      if (Date.now() - lastRefreshAt < refreshIntervalMs) return;
      refreshing = true;
      try {
        const response = await fetch("/api/auth/refresh", { method: "POST", cache: "no-store" });
        if (response.ok) {
          lastRefreshAt = Date.now();
          router.refresh();
          return;
        }
        if (response.status === 401) window.location.replace("/login");
      } finally {
        refreshing = false;
      }
    }

    recordActivity();
    const interval = window.setInterval(() => void refreshSession(), 60 * 1000);
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") recordActivity();
    };
    const activityEvents: Array<keyof WindowEventMap> = ["pointerdown", "keydown", "scroll", "touchstart"];
    document.addEventListener("visibilitychange", handleVisibilityChange);
    activityEvents.forEach((event) => window.addEventListener(event, recordActivity, { passive: true }));
    return () => {
      window.removeEventListener("pageshow", revalidateRestoredPage);
      window.removeEventListener("api:session-expired", handleExpiredSession);
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      activityEvents.forEach((event) => window.removeEventListener(event, recordActivity));
    };
  }, [persistentSession, router]);

  return null;
}
