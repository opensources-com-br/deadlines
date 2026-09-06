import Link from "next/link";
import type { ReactNode } from "react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type AuthShellProps = {
  title: string;
  description: string;
  children: ReactNode;
};

export function AuthShell({ title, description, children }: AuthShellProps) {
  return (
    <main className="flex min-h-svh w-full items-center justify-center bg-muted/30 p-6 text-foreground md:p-10">
      <div className="w-full max-w-sm">
        <Link href="/" className="mb-6 inline-flex text-lg font-semibold tracking-tight">
          Deadlines
        </Link>
        <Card className="w-full">
          <CardHeader>
            <CardTitle className="text-2xl font-semibold tracking-tight">{title}</CardTitle>
            <CardDescription className="text-base leading-7">{description}</CardDescription>
          </CardHeader>
          <CardContent>{children}</CardContent>
        </Card>
      </div>
    </main>
  );
}
