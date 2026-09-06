import Link from "next/link";

import { Button, buttonVariants } from "@/components/ui/button";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";

type VerifyEmailScreenProps = {
  hasToken: boolean;
};

export function VerifyEmailScreen({ hasToken }: VerifyEmailScreenProps) {
  if (!hasToken) {
    return (
      <AuthShell title="This link is invalid" description="Request a new confirmation email and try again.">
        <Link
          href="/check-email"
          className={buttonVariants({ size: "lg" })}
        >
          Send a new link
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Confirm your email" description="Confirm your email address to activate your Deadlines account.">
      <div className="rounded-xl border bg-muted/50 p-5 text-sm leading-6 text-muted-foreground">
        Your confirmation link is ready. Once confirmed, you can sign in to your account.
      </div>
      <Button className="mt-5 w-full" size="lg">Confirm email</Button>
    </AuthShell>
  );
}
