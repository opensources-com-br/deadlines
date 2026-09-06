import { CheckEmailScreen } from "@/features/identity/presentation/screens/CheckEmailScreen";

type CheckEmailPageProps = {
  searchParams: Promise<{ email?: string | string[]; next?: string | string[] }>;
};

export default async function CheckEmailPage({ searchParams }: CheckEmailPageProps) {
  const { email, next } = await searchParams;
  const nextPath = typeof next === "string" && next.startsWith("/") && !next.startsWith("//") ? next : undefined;

  return <CheckEmailScreen email={typeof email === "string" ? email : undefined} nextPath={nextPath} />;
}
