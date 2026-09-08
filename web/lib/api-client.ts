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
    INVALID_CREDENTIALS: "Your email or password is incorrect.",
    INVALID_CURRENT_PASSWORD: "Your current password is incorrect.",
    USER_ALREADY_EXISTS: "An account with this email already exists.",
    ORGANIZATION_ALREADY_EXISTS: "You already have an organization.",
    ACTIVE_MEMBERSHIP_ALREADY_EXISTS: "This user already belongs to the organization.",
    ORGANIZATION_ACCESS_DENIED: "You do not have access to this organization.",
    ACCOUNT_NOT_ACTIVE: "This account is not active.",
    ACCOUNT_OWNER_CONFLICT: "Transfer organization ownership before continuing.",
    ORGANIZATION_STATE_CONFLICT: "This action is not available for the organization’s current state.",
    VALIDATION_ERROR: "Please review the highlighted fields.",
    INVALID_REQUEST: "Please review your request and try again.",
    BACKEND_UNAVAILABLE: "The service is temporarily unavailable. Please try again.",
    default: "Something went wrong. Please try again.",
  },
  "pt-BR": {
    UNAUTHORIZED: "Sua sessão terminou. Entre novamente.",
    FORBIDDEN: "Você não tem permissão para realizar esta ação.",
    INVALID_CREDENTIALS: "Seu e-mail ou senha está incorreto.",
    INVALID_CURRENT_PASSWORD: "Sua senha atual está incorreta.",
    USER_ALREADY_EXISTS: "Já existe uma conta com este e-mail.",
    ORGANIZATION_ALREADY_EXISTS: "Você já possui uma organização.",
    ACTIVE_MEMBERSHIP_ALREADY_EXISTS: "Esta pessoa já pertence à organização.",
    ORGANIZATION_ACCESS_DENIED: "Você não tem acesso a esta organização.",
    ACCOUNT_NOT_ACTIVE: "Esta conta não está ativa.",
    ACCOUNT_OWNER_CONFLICT: "Transfira a propriedade da organização antes de continuar.",
    ORGANIZATION_STATE_CONFLICT: "Esta ação não está disponível para o estado atual da organização.",
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
    response = await fetch(path, withCsrfToken(options));
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

function withCsrfToken(options: RequestInit): RequestInit {
  if (typeof document === "undefined" || !options.method || !["POST", "PUT", "PATCH", "DELETE"].includes(options.method)) {
    return options;
  }

  const token = document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith("opensources_csrf_token="))
    ?.split("=")[1];
  if (!token) return options;

  const headers = new Headers(options.headers);
  headers.set("X-CSRF-Token", decodeURIComponent(token));
  return { ...options, headers };
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
