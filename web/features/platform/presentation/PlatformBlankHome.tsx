"use client";

import { useState } from "react";
import { toast } from "sonner";

import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformSidebar } from "@/features/platform/presentation/PlatformSidebar";

type PlatformBlankHomeProps = {
  user: UserProfile;
};

export function PlatformBlankHome({ user }: PlatformBlankHomeProps) {
  const [isSigningOut, setIsSigningOut] = useState(false);

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    window.location.replace("/login");
  }

  return (
    <SidebarProvider>
      <PlatformSidebar
        user={user}
        onSignOut={() => void handleSignOut()}
        isSigningOut={isSigningOut}
      />
      <SidebarInset className="min-h-svh bg-background text-foreground">
        <header className="w-full border-b border-border py-4">
          <div className="flex w-full items-center px-[30px]">
            <SidebarTrigger variant="ghost" aria-label="Toggle sidebar" />
          </div>
        </header>
      </SidebarInset>
    </SidebarProvider>
  );
}
