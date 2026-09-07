import { cookies, headers } from "next/headers";
import { getRequestConfig } from "next-intl/server";

import { defaultLocale, isAppLocale, localeCookieName } from "@/i18n/config";

export default getRequestConfig(async () => {
  const cookieLocale = (await cookies()).get(localeCookieName)?.value;
  const acceptedLanguages = (await headers()).get("accept-language") ?? "";
  const acceptedLanguagesLower = acceptedLanguages.toLowerCase();
  const browserLocale = acceptedLanguagesLower.includes("pt")
    ? "pt-BR"
    : acceptedLanguagesLower.includes("en") ? "en" : defaultLocale;
  const locale = isAppLocale(cookieLocale) ? cookieLocale : browserLocale;

  return {
    locale,
    messages: (await import(`../messages/${locale}.json`)).default,
  };
});
