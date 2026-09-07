"use client";

import { useEffect } from "react";

export function PlatformSessionGuard() {
  useEffect(() => {
    function revalidateRestoredPage(event: PageTransitionEvent) {
      const navigation = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
      if (event.persisted || navigation?.type === "back_forward") window.location.reload();
    }

    window.addEventListener("pageshow", revalidateRestoredPage);
    return () => window.removeEventListener("pageshow", revalidateRestoredPage);
  }, []);

  return null;
}
