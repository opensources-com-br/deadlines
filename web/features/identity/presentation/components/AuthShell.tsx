import Link from "next/link";
import type { ReactNode } from "react";

type AuthShellProps = {
  title: string;
  description: string;
  children: ReactNode;
};

export function AuthShell({ title, description, children }: AuthShellProps) {
  return (
    <main className="min-h-screen bg-white px-6 py-6 text-zinc-950">
      <Link href="/" className="inline-flex text-lg font-semibold tracking-tight">
        Deadlines
      </Link>
      <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-md items-center">
        <section className="w-full">
          <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-3 text-base leading-7 text-zinc-600">{description}</p>
          <div className="mt-8">{children}</div>
        </section>
      </div>
    </main>
  );
}
