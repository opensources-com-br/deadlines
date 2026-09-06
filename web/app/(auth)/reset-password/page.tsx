import { ResetPasswordScreen } from "@/features/identity/presentation/screens/ResetPasswordScreen";

type ResetPasswordPageProps = {
  searchParams: Promise<{ token?: string | string[] }>;
};

export default async function ResetPasswordPage({ searchParams }: ResetPasswordPageProps) {
  const { token } = await searchParams;
  const resetToken = typeof token === "string" ? token : undefined;

  return <ResetPasswordScreen token={resetToken} />;
}
