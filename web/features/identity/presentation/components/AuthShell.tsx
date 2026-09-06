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
    <main className="min-h-screen bg-muted/30 px-6 py-6 text-foreground">
      <Link href="/" className="inline-flex text-lg font-semibold tracking-tight">
        Deadlines
      </Link>
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-md items-center">
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
