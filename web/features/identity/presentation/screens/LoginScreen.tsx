import Link from "next/link";

import { AuthShell } from "@/features/identity/presentation/components/AuthShell";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";

export function LoginScreen() {
  return (
    <AuthShell title="Welcome back" description="Enter your details to access your workspace.">
      <form className="grid gap-5">
        <Input id="email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <div className="grid gap-2">
          <div className="flex items-center justify-between gap-4">
            <label className="text-sm font-medium text-zinc-800" htmlFor="password">
              Password
            </label>
            <Link href="/forgot-password" className="text-sm font-medium text-zinc-700 underline underline-offset-4">
              Forgot password?
            </Link>
          </div>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            placeholder="Enter your password"
            required
            className="h-11 rounded-lg border border-zinc-300 bg-white px-3 text-base text-zinc-950 outline-none transition placeholder:text-zinc-400 focus:border-zinc-950 focus:ring-2 focus:ring-zinc-950/10"
          />
        </div>
        <Button className="mt-1 w-full">
          Sign in
        </Button>
      </form>
      <p className="mt-6 text-sm text-zinc-600">
        New to Deadlines?{" "}
        <Link href="/register" className="font-medium text-zinc-950 underline underline-offset-4">
          Create an account
        </Link>
      </p>
    </AuthShell>
  );
}
