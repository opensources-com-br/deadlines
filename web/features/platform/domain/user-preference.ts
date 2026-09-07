export type UserPreference = {
  locale: "pt-BR" | "en";
  timezone: string;
  theme: "light" | "dark" | "system";
  updatedAt: string;
};

export type UpdateUserPreference = Partial<Pick<UserPreference, "locale" | "timezone" | "theme">>;
