export type UserSession = {
  id: string;
  deviceId: string;
  userAgent: string | null;
  ipAddress: string | null;
  expiresAt: string;
  createdAt: string;
  lastSeenAt: string;
  isCurrent: boolean;
};

export type SessionList = {
  data: UserSession[];
};
