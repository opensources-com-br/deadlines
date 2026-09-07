import type { ReactNode } from "react";
import { cookies } from "next/headers";

import { PlatformSessionGuard } from "@/features/platform/presentation/PlatformSessionGuard";

type PlatformLayoutProps = {
  children: ReactNode;
};

export default async function PlatformLayout({ children }: PlatformLayoutProps) {
  const persistentSession = (await cookies()).get("deadlines_persistent_session")?.value === "true";
  return (
    <>
      <PlatformSessionGuard persistentSession={persistentSession} />
      {children}
    </>
  );
}
