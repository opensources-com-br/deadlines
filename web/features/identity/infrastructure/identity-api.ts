import { apiClient, ApiClientError } from "@/lib/api-client";

type RegisterInput = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

type LoginInput = {
  email: string;
  password: string;
  keepSignedIn: boolean;
};

const apiBaseUrl = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

const post = <TResponse>(path: string, body: unknown, baseUrl = apiBaseUrl) =>
  apiClient.post<TResponse | undefined>(`${baseUrl}${path}`, body);

export const identityApi = {
  register: (input: RegisterInput) => post("/api/v1/auth/register", input),
  login: (input: LoginInput) => post("/api/auth/login", input, ""),
  resendVerification: (email: string) => post("/api/v1/auth/email/resend", { email }),
  requestPasswordReset: (email: string) => post("/api/v1/auth/forgot-password", { email }),
  resetPassword: (token: string, password: string) => post("/api/v1/auth/reset-password", { token, password }),
  verifyEmail: (token: string) => post("/api/v1/auth/email/verify", { token }),
};

export function identityErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.message;
  }

  return "Something went wrong. Please try again.";
}
