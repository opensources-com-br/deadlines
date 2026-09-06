import { ForgotPasswordScreen } from "@/features/identity/presentation/screens/ForgotPasswordScreen";

type ForgotPasswordPageProps = {
  searchParams: Promise<{ email?: string | string[] }>;
};

export default async function ForgotPasswordPage({ searchParams }: ForgotPasswordPageProps) {
  const { email } = await searchParams;
  const initialEmail = typeof email === "string" ? email : undefined;

  return <ForgotPasswordScreen initialEmail={initialEmail} />;
}
