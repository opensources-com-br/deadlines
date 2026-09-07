"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformSidebar, type SettingsSection } from "@/features/platform/presentation/PlatformSidebar";

type PlatformBlankHomeProps = {
  user: UserProfile;
};

export function PlatformBlankHome({ user }: PlatformBlankHomeProps) {
  const router = useRouter();
  const [isSigningOut, setIsSigningOut] = useState(false);

  function handleSettingsNavigation(section: SettingsSection) {
    router.push(`/app/settings?section=${section}`);
  }

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    router.replace("/login");
    router.refresh();
  }

  return (
    <SidebarProvider>
      <PlatformSidebar
        user={user}
        onSettingsSelect={handleSettingsNavigation}
        onSignOut={() => void handleSignOut()}
        isSigningOut={isSigningOut}
      />
      <SidebarInset className="min-h-svh bg-background text-foreground">
        <header className="w-full py-4">
          <div className="flex w-full items-center px-[30px]">
            <SidebarTrigger variant="ghost" size="icon-sm" aria-label="Toggle sidebar" />
          </div>
        </header>
      </SidebarInset>
    </SidebarProvider>
  );
}
