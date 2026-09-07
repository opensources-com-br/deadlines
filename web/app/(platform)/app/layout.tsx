import type { ReactNode } from "react";

import { PlatformSessionGuard } from "@/features/platform/presentation/PlatformSessionGuard";

type PlatformLayoutProps = {
  children: ReactNode;
};

export default function PlatformLayout({ children }: PlatformLayoutProps) {
  return (
    <>
      <PlatformSessionGuard />
      {children}
    </>
  );
}
