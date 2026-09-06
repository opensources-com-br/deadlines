const backendApiBaseUrl = (process.env.BACKEND_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

export function backendApiUrl(path: string): string {
  return `${backendApiBaseUrl}${path}`;
}
