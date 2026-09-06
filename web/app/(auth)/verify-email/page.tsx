import { cookies } from "next/headers";

import { VerifyEmailScreen } from "@/features/identity/presentation/screens/VerifyEmailScreen";

type VerifyEmailPageProps = {
  searchParams: Promise<{ token?: string | string[] }>;
};

export default async function VerifyEmailPage({ searchParams }: VerifyEmailPageProps) {
  const { token } = await searchParams;
  const verificationToken = typeof token === "string" ? token : undefined;
  const hasInvitation = Boolean((await cookies()).get("deadlines_invitation_token")?.value);

  return <VerifyEmailScreen token={verificationToken} hasInvitation={hasInvitation} />;
}
