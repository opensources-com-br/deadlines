import Image from "next/image";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";

export function LandingPage() {
  return (
    <main className="min-h-screen bg-[#0a0a0a] px-5 text-foreground sm:px-6">
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
    </main>
  );
}
