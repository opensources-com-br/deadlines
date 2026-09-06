import Link from "next/link";
import type { ReactNode } from "react";
import { ArrowLeft } from "lucide-react";

import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type AuthShellProps = {
  title: string;
  description: string;
  children: ReactNode;
};

export function AuthShell({ title, description, children }: AuthShellProps) {
  return (
    <main className="relative flex min-h-svh w-full items-center justify-center bg-muted/30 p-6 text-foreground md:p-10">
      <Link
        href="/"
        aria-label="Back to home"
        className={buttonVariants({ variant: "ghost", size: "icon" }) + " absolute left-6 top-6 md:left-10 md:top-10"}
      >
        <ArrowLeft />
      </Link>
      <div className="w-full max-w-sm">
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
