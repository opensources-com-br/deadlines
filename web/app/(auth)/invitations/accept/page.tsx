import { cookies } from "next/headers";

import { AcceptInvitationScreen } from "@/features/team/presentation/AcceptInvitationScreen";

type AcceptInvitationPageProps = {
  searchParams: Promise<{ token?: string | string[] }>;
};

export default async function AcceptInvitationPage({ searchParams }: AcceptInvitationPageProps) {
  const { token } = await searchParams;
  const authenticated = Boolean((await cookies()).get("deadlines_access_token")?.value);
  return (
    <AcceptInvitationScreen
      token={typeof token === "string" ? token : undefined}
      authenticated={authenticated}
    />
  );
}
