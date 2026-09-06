import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button } from "@/shared/ui/Button";

export function CheckEmailScreen() {
  return (
    <AuthShell title="Check your inbox" description="We sent a confirmation link to your email address.">
      <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-5 text-sm leading-6 text-zinc-600">
        Your account will be ready once you confirm your email. The link expires after a limited time for your security.
      </div>
      <Button className="mt-5 w-full">Resend confirmation email</Button>
      <p className="mt-6 text-sm text-zinc-600">
        Already confirmed your email?{" "}
        <Link href="/login" className="font-medium text-zinc-950 underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
