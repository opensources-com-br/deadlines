import { LoginScreen } from "@/features/identity/presentation/screens/LoginScreen";

type LoginPageProps = {
  searchParams: Promise<{ next?: string | string[] }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const { next } = await searchParams;
  const requestedPath = typeof next === "string" && next.startsWith("/") && !next.startsWith("//") ? next : undefined;
  return <LoginScreen nextPath={requestedPath} />;
}
