import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default async function ContinueInvitationPage() {
  const token = (await cookies()).get("deadlines_invitation_token")?.value;
  if (!token) redirect("/login");
  redirect(`/invitations/accept?token=${encodeURIComponent(token)}`);
}
