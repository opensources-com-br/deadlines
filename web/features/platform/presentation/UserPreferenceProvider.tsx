"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useTheme } from "next-themes";

import type { UserPreference } from "@/features/platform/domain/user-preference";

type PreferenceContextValue = { preferences: UserPreference; setPreferences: (value: UserPreference) => void };
const PreferenceContext = createContext<PreferenceContextValue | null>(null);

export function UserPreferenceProvider({ initialPreferences, children }: { initialPreferences: UserPreference; children: ReactNode }) {
  const [preferences, setPreferences] = useState(initialPreferences);
  const { setTheme } = useTheme();
  useEffect(() => setTheme(preferences.theme), [preferences.theme, setTheme]);
  return <PreferenceContext.Provider value={{ preferences, setPreferences }}>{children}</PreferenceContext.Provider>;
}

export function useUserPreferences() {
  const value = useContext(PreferenceContext);
  if (!value) throw new Error("useUserPreferences must be used inside UserPreferenceProvider");
  return value;
}
