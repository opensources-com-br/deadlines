export const locales = ["pt-BR", "en"] as const;
export type AppLocale = (typeof locales)[number];

export const defaultLocale: AppLocale = "pt-BR";
export const localeCookieName = "deadlines_locale";

export function isAppLocale(value: unknown): value is AppLocale {
  return typeof value === "string" && locales.includes(value as AppLocale);
}
