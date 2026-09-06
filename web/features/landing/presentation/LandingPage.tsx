import Image from "next/image";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";

export function LandingPage() {
  return (
    <main className="flex min-h-screen flex-col bg-white px-6 text-zinc-950">
      <header className="sticky top-5 z-10 mx-auto mt-5 flex w-full max-w-6xl items-center justify-between rounded-2xl border border-zinc-200 bg-white/90 px-4 py-3 shadow-sm backdrop-blur md:px-5">
        <Link href="/" className="flex items-center gap-2 text-lg font-semibold tracking-tight" aria-label="Deadlines home">
          <span className="flex size-8 items-center justify-center" aria-hidden="true">
            <Image src="/deadlines-mark.png" alt="" width={32} height={32} priority />
          </span>
          Deadlines
        </Link>
        <Link href="/login" className={buttonVariants({ size: "lg" }) + " h-10 rounded-full px-5 sm:px-7"}>
          Get started
        </Link>
      </header>

      <section className="mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center py-20">
        <div className="max-w-2xl">
          <p className="mb-5 text-sm font-medium text-zinc-500">Organize what matters.</p>
          <h1 className="text-balance text-5xl font-semibold tracking-tight text-zinc-950 sm:text-6xl">
            Everything important, in one place.
          </h1>
          <p className="mt-6 max-w-xl text-pretty text-lg leading-8 text-zinc-600">
            A calmer way to keep your work, relationships, and next steps moving forward.
          </p>
          <div className="mt-9">
            <Link href="/login" className={buttonVariants({ size: "lg" })}>
              Get started
            </Link>
          </div>
        </div>
      </section>

      <footer className="mx-auto flex w-full max-w-6xl py-6 text-sm text-zinc-500">
        © {new Date().getFullYear()} Deadlines
      </footer>
    </main>
  );
}
