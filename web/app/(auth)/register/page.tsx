import { RegisterScreen } from "@/features/identity/presentation/screens/RegisterScreen";

type RegisterPageProps = {
  searchParams: Promise<{ email?: string | string[]; next?: string | string[] }>;
};

export default async function RegisterPage({ searchParams }: RegisterPageProps) {
  const { email, next } = await searchParams;
  const nextPath = typeof next === "string" && next.startsWith("/") && !next.startsWith("//") ? next : undefined;
  return (
    <RegisterScreen
      initialEmail={typeof email === "string" ? email : undefined}
      nextPath={nextPath}
    />
  );
}
