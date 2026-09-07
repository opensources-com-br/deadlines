"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";

import type { AuthorizationContext } from "@/features/access/domain/authorization";

type AuthorizationValue = Omit<AuthorizationContext, "permissions"> & {
  permissions: ReadonlySet<string>;
};

const Context = createContext<AuthorizationValue | null>(null);

export function AuthorizationProvider({
  authorization,
  children,
}: {
  authorization: AuthorizationContext;
  children: ReactNode;
}) {
  const value = useMemo(
    () => ({
      organizationId: authorization.organizationId,
      membershipId: authorization.membershipId,
      roleId: authorization.roleId,
      permissions: new Set(authorization.permissions),
    }),
    [authorization],
  );

  return <Context value={value}>{children}</Context>;
}

export function useAuthorization() {
  const context = useContext(Context);
  if (!context) throw new Error("AuthorizationProvider is missing.");
  return context;
}

export function usePermission(permission: string) {
  return useAuthorization().permissions.has(permission);
}

export function useAnyPermission(permissions: string[]) {
  const authorization = useAuthorization();
  return permissions.some((permission) => authorization.permissions.has(permission));
}
