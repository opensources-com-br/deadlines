export type ApiErrorPayload = {
  error?: {
    code?: string;
    fields?: Record<string, string>;
    requestId?: string;
  };
};

export class ApiClientError extends Error {
  constructor(
    readonly status: number,
    readonly code: string | undefined,
    readonly fields: Record<string, string>,
    readonly requestId: string | undefined,
  ) {
    super(apiErrorMessage(code, status));
    this.name = "ApiClientError";
  }
}

const messages = {
  en: {
    UNAUTHORIZED: "Your session has ended. Please sign in again.",
    FORBIDDEN: "You do not have permission to perform this action.",
    VALIDATION_ERROR: "Please review the highlighted fields.",
    INVALID_REQUEST: "Please review your request and try again.",
    BACKEND_UNAVAILABLE: "The service is temporarily unavailable. Please try again.",
    default: "Something went wrong. Please try again.",
  },
  "pt-BR": {
    UNAUTHORIZED: "Sua sessão terminou. Entre novamente.",
    FORBIDDEN: "Você não tem permissão para realizar esta ação.",
    VALIDATION_ERROR: "Revise os campos destacados.",
    INVALID_REQUEST: "Revise sua solicitação e tente novamente.",
    BACKEND_UNAVAILABLE: "O serviço está indisponível no momento. Tente novamente.",
    default: "Algo deu errado. Tente novamente.",
  },
} as const;

function apiErrorMessage(code: string | undefined, status: number): string {
  const locale = typeof document !== "undefined" && document.documentElement.lang === "en" ? "en" : "pt-BR";
  const messageCode = code ?? (status === 401 ? "UNAUTHORIZED" : status === 403 ? "FORBIDDEN" : undefined);
  return messages[locale][messageCode as keyof typeof messages.en] ?? messages[locale].default;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;

  try {
    response = await fetch(path, options);
  } catch {
    throw new ApiClientError(0, "BACKEND_UNAVAILABLE", {}, undefined);
  }

  if (response.status === 204) return undefined as T;

  const data = (await response.json().catch(() => ({}))) as T & ApiErrorPayload;
  if (response.ok) return data;

  const error = data.error;
  if (response.status === 401 && typeof window !== "undefined") {
    window.dispatchEvent(new Event("api:session-expired"));
  }
  throw new ApiClientError(response.status, error?.code, error?.fields ?? {}, error?.requestId);
}

function jsonRequest(method: string, body: unknown, options?: RequestInit): RequestInit {
  return {
    ...options,
    method,
    headers: { "Content-Type": "application/json", ...options?.headers },
    body: JSON.stringify(body),
  };
}

export const apiClient = {
  get: <T>(path: string, options?: RequestInit) => request<T>(path, options),
  post: <T>(path: string, body?: unknown, options?: RequestInit) => request<T>(path, jsonRequest("POST", body, options)),
  patch: <T>(path: string, body?: unknown, options?: RequestInit) => request<T>(path, jsonRequest("PATCH", body, options)),
  delete: <T>(path: string, options?: RequestInit) => request<T>(path, { ...options, method: "DELETE" }),
  put: <T>(path: string, body?: unknown, options?: RequestInit) => request<T>(path, jsonRequest("PUT", body, options)),
};
