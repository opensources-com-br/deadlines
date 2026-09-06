import Link from "next/link";

import { Button } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";

export function CheckEmailScreen() {
  return (
    <AuthShell title="Check your inbox" description="We sent a confirmation link to your email address.">
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        Your account will be ready once you confirm your email. The link expires after a limited time for your security.
      </div>
      <Button className="mt-5 w-full" size="lg">Resend confirmation email</Button>
      <p className="mt-6 text-sm text-muted-foreground">
        Already confirmed your email?{" "}
        <Link href="/login" className="font-medium text-foreground underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
