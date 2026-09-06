type ErrorPayload = {
  error?: {
    code?: string;
    message?: string;
  };
};

export class IdentityApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = "IdentityApiError";
  }
}

type RegisterInput = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

type LoginInput = {
  email: string;
  password: string;
};

const apiBaseUrl = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

async function post<TResponse>(path: string, body: unknown, baseUrl = apiBaseUrl): Promise<TResponse | undefined> {
  let response: Response;

  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    throw new IdentityApiError("We could not reach the server. Start the local backend and try again.", 0);
  }

  if (response.ok) {
    if (response.status === 204) {
      return undefined;
    }

    return (await response.json()) as TResponse;
  }

  const payload = (await response.json().catch(() => ({}))) as ErrorPayload;
  throw new IdentityApiError(
    payload.error?.message ?? "Something went wrong. Please try again.",
    response.status,
    payload.error?.code,
  );
}

export const identityApi = {
  register: (input: RegisterInput) => post("/api/v1/auth/register", input),
  login: (input: LoginInput) => post("/api/auth/login", input, ""),
  resendVerification: (email: string) => post("/api/v1/auth/email/resend", { email }),
  requestPasswordReset: (email: string) => post("/api/v1/auth/forgot-password", { email }),
  resetPassword: (token: string, password: string) => post("/api/v1/auth/reset-password", { token, password }),
  verifyEmail: (token: string) => post("/api/v1/auth/email/verify", { token }),
};

export function identityErrorMessage(error: unknown): string {
  if (error instanceof IdentityApiError) {
    return error.message;
  }

  return "Something went wrong. Please try again.";
}
