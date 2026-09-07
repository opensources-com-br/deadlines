"use client";

import type { ReactNode } from "react";

import { usePermission } from "@/features/access/presentation/AuthorizationProvider";

export function Can({ permission, children, fallback = null }: {
  permission: string;
  children: ReactNode;
  fallback?: ReactNode;
}) {
  return usePermission(permission) ? children : fallback;
}
