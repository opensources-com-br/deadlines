import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button } from "@/shared/ui/Button";

type VerifyEmailScreenProps = {
  hasToken: boolean;
};

export function VerifyEmailScreen({ hasToken }: VerifyEmailScreenProps) {
  if (!hasToken) {
    return (
      <AuthShell title="This link is invalid" description="Request a new confirmation email and try again.">
        <Link
          href="/check-email"
          className="inline-flex h-11 items-center justify-center rounded-lg bg-zinc-950 px-5 text-sm font-medium text-white"
        >
          Send a new link
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Confirm your email" description="Confirm your email address to activate your Deadlines account.">
      <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-5 text-sm leading-6 text-zinc-600">
        Your confirmation link is ready. Once confirmed, you can sign in to your account.
      </div>
      <Button className="mt-5 w-full">Confirm email</Button>
    </AuthShell>
  );
}
