export type AuditEvent = {
  id: string;
  organizationId: string;
  actorId?: string | null;
  occurredAt: string;
  action: string;
  resource: string;
  resourceId: string;
  metadata: Record<string, string | boolean>;
};

export type AuditPage = {
  data: AuditEvent[];
  offset: number;
  limit: number;
  hasMore: boolean;
};
