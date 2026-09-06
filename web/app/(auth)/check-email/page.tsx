import { CheckEmailScreen } from "@/features/identity/presentation/screens/CheckEmailScreen";

type CheckEmailPageProps = {
  searchParams: Promise<{ email?: string | string[] }>;
};

export default async function CheckEmailPage({ searchParams }: CheckEmailPageProps) {
  const { email } = await searchParams;

  return <CheckEmailScreen email={typeof email === "string" ? email : undefined} />;
}
