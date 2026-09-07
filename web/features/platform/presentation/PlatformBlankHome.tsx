"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformSidebar } from "@/features/platform/presentation/PlatformSidebar";

type PlatformBlankHomeProps = {
  user: UserProfile;
};

export function PlatformBlankHome({ user }: PlatformBlankHomeProps) {
  const t = useTranslations("Public");
  const [isSigningOut, setIsSigningOut] = useState(false);

  function handleSignOut() {
    setIsSigningOut(true);
    window.location.replace("/api/auth/logout");
  }

  return (
    <SidebarProvider>
      <PlatformSidebar
        user={user}
        onSignOut={handleSignOut}
        isSigningOut={isSigningOut}
      />
      <SidebarInset className="min-h-svh bg-background text-foreground">
        <header className="w-full border-b border-border py-4">
          <div className="flex w-full items-center px-[30px]">
            <SidebarTrigger variant="ghost" aria-label={t("toggleSidebar")} />
          </div>
        </header>
      </SidebarInset>
    </SidebarProvider>
  );
}
