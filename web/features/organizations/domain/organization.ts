export type Organization = {
  id: string;
  name: string;
  slug: string;
  role: "owner" | "member";
  createdAt: string;
  updatedAt: string;
};

export type CreateOrganizationInput = {
  name: string;
  slug: string;
};

export type UpdateOrganizationInput = Partial<CreateOrganizationInput>;
