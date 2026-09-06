import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function GET(request: Request) {
  const query = new URL(request.url).searchParams.toString();
  return forwardAccessRequest(`/api/v1/audits${query ? `?${query}` : ""}`, "GET");
}
