import { VerifyEmailScreen } from "@/features/identity/presentation/screens/VerifyEmailScreen";

type VerifyEmailPageProps = {
  searchParams: Promise<{ token?: string | string[] }>;
};

export default async function VerifyEmailPage({ searchParams }: VerifyEmailPageProps) {
  const { token } = await searchParams;
  const verificationToken = typeof token === "string" ? token : undefined;

  return <VerifyEmailScreen token={verificationToken} />;
}
