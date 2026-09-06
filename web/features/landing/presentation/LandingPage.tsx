import Image from "next/image";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";

export function LandingPage() {
  return (
    <main className="flex min-h-screen min-w-0 flex-col overflow-hidden bg-[#0a0a0a] px-5 text-foreground sm:px-6">
      <header className="sticky top-5 z-10 mx-auto mt-5 flex w-full max-w-7xl items-center justify-between rounded-2xl border border-white/10 bg-black/60 px-4 py-3 shadow-2xl shadow-black/20 backdrop-blur-xl md:px-5">
        <Link href="/" className="flex items-center gap-2 text-lg font-semibold tracking-tight" aria-label="Deadlines home">
          <span className="flex size-8 items-center justify-center" aria-hidden="true">
            <Image className="invert" src="/deadlines-mark.png" alt="" width={32} height={32} priority />
          </span>
          Deadlines
        </Link>
        <Link href="/login" className={buttonVariants({ size: "lg" }) + " h-10 rounded-full px-5 sm:px-7"}>
          Get started
        </Link>
      </header>

      <section className="mx-auto grid min-w-0 w-full max-w-7xl flex-1 items-center gap-8 py-14 md:grid-cols-[minmax(0,0.9fr)_minmax(420px,1.1fr)] md:py-8">
        <div className="relative z-1 min-w-0 max-w-2xl md:py-20">
          <p className="mb-6 text-sm font-medium tracking-wide text-zinc-400">Deadlines, without the noise.</p>
          <h1 className="text-balance text-5xl font-semibold leading-[0.98] tracking-[-0.045em] text-white sm:text-6xl lg:text-7xl">
            Stay ahead of what matters.
          </h1>
          <p className="mt-7 max-w-lg text-pretty text-base leading-7 text-zinc-400 sm:text-lg">
            One calm place to organize deadlines, follow-ups, and the work that keeps moving.
          </p>
          <div className="mt-10">
            <Link href="/login" className={buttonVariants({ size: "lg" }) + " rounded-full px-7"}>
              Get started
            </Link>
          </div>
        </div>

        <div className="relative order-first flex min-h-72 min-w-0 items-center justify-center md:order-last md:min-h-[620px]" aria-hidden="true">
          <div className="absolute inset-1/4 rounded-full bg-white/4 blur-3xl" />
          <Image
            className="relative h-auto w-full max-w-[620px] object-contain mix-blend-lighten"
            src="/deadlines-mark-3d.png"
            alt=""
            width={1254}
            height={1254}
            priority
          />
        </div>
      </section>

      <footer className="mx-auto flex w-full max-w-7xl border-t border-white/8 py-6 text-sm text-zinc-500">
        © {new Date().getFullYear()} Deadlines
      </footer>
    </main>
  );
}
