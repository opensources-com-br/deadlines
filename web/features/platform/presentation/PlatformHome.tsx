"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";

type PlatformHomeProps = {
  user: {
    firstName: string;
    lastName: string;
    email: string;
  };
};

export function PlatformHome({ user }: PlatformHomeProps) {
  const router = useRouter();
  const [isSigningOut, setIsSigningOut] = useState(false);

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    router.replace("/login");
    router.refresh();
  }

  return (
    <main className="min-h-svh bg-background text-foreground">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
        <div className="flex items-center gap-2 text-lg font-semibold tracking-tight">
          <Image className="size-8 invert" src="/deadlines-mark.png" alt="" width={32} height={32} priority />
          Deadlines
        </div>
        <Button variant="outline" type="button" onClick={handleSignOut} disabled={isSigningOut}>
          {isSigningOut ? "Signing out..." : "Log out"}
        </Button>
      </header>

      <section className="mx-auto flex w-full max-w-6xl px-6 py-20">
        <div className="max-w-2xl">
          <p className="text-sm font-medium text-muted-foreground">Platform</p>
          <h1 className="mt-4 text-4xl font-semibold tracking-tight sm:text-5xl">
            Welcome, {user.firstName}.
          </h1>
          <p className="mt-5 text-lg leading-8 text-muted-foreground">
            Your account is active and ready for the next part of Deadlines.
          </p>
          <p className="mt-8 text-sm text-muted-foreground">Signed in as {user.email}</p>
        </div>
      </section>
    </main>
  );
}
