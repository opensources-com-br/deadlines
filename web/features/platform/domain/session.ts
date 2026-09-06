export type UserSession = {
  id: string;
  userAgent: string | null;
  ipAddress: string | null;
  expiresAt: string;
  createdAt: string;
  isCurrent: boolean;
};

export type SessionList = {
  data: UserSession[];
};
