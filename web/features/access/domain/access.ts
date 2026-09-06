export type Permission = {
  id: string;
  key: string;
  name: string;
  description?: string;
  isSystem: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Role = {
  id: string;
  key: string;
  name: string;
  description?: string;
  isSystem: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AccessList<T> = {
  data: T[];
};

export type AccessInput = {
  key: string;
  name: string;
  description?: string;
};
