import { Button } from "@/shared/ui/Button";

export function LandingPage() {
  return (
    <main className="flex min-h-screen flex-col bg-white px-6 text-zinc-950">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between py-6">
        <span className="text-lg font-semibold tracking-tight">Deadlines</span>
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
            <Button>Get started</Button>
          </div>
        </div>
      </section>

      <footer className="mx-auto flex w-full max-w-6xl py-6 text-sm text-zinc-500">
        © {new Date().getFullYear()} Deadlines
      </footer>
    </main>
  );
}
