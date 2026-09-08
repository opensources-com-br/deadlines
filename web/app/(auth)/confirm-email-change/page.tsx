import { ConfirmEmailChangeScreen } from "@/features/identity/presentation/screens/ConfirmEmailChangeScreen";

type ConfirmEmailChangePageProps = { searchParams: Promise<{ token?: string | string[] }> };

export default async function ConfirmEmailChangePage({ searchParams }: ConfirmEmailChangePageProps) {
  const { token } = await searchParams;
  return <ConfirmEmailChangeScreen token={typeof token === "string" ? token : undefined} />;
}
