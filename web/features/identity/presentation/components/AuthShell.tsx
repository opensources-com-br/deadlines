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
        <Card className="w-full py-8">
          <CardHeader className="px-8">
            <CardTitle>{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </CardHeader>
          <CardContent className="px-8">{children}</CardContent>
        </Card>
      </div>
    </main>
  );
}
