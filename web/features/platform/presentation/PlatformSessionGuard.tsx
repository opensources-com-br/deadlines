"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

const refreshIntervalMs = 14 * 60 * 1000;

export function PlatformSessionGuard() {
  const router = useRouter();

  useEffect(() => {
    function revalidateRestoredPage(event: PageTransitionEvent) {
      const navigation = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
      if (event.persisted || navigation?.type === "back_forward") window.location.reload();
    }

    window.addEventListener("pageshow", revalidateRestoredPage);

    let refreshing = false;
    async function refreshSession() {
      if (refreshing) return;
      refreshing = true;
      try {
        const response = await fetch("/api/auth/refresh", { method: "POST", cache: "no-store" });
        if (response.ok) {
          router.refresh();
          return;
        }
        if (response.status === 401) window.location.replace("/login");
      } finally {
        refreshing = false;
      }
    }

    const interval = window.setInterval(() => void refreshSession(), refreshIntervalMs);
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") void refreshSession();
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      window.removeEventListener("pageshow", revalidateRestoredPage);
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [router]);

  return null;
}
