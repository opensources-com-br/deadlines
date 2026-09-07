export type Organization = {
  id: string;
  name: string;
  slug: string;
  role: "owner" | "member";
  status: "active" | "suspended";
  createdAt: string;
  updatedAt: string;
};

export type CreateOrganizationInput = {
  name: string;
  slug: string;
};

export type UpdateOrganizationInput = Partial<CreateOrganizationInput>;
