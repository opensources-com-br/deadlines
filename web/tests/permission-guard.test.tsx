import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Can } from "@/features/access/presentation/Can";
import { AuthorizationProvider } from "@/features/access/presentation/AuthorizationProvider";

const authorization = {
  organizationId: "organization",
  membershipId: "membership",
  roleId: "role",
  permissions: ["members.read"],
};

describe("Can", () => {
  it("renders protected content only for an assigned permission", () => {
    render(
      <AuthorizationProvider authorization={authorization}>
        <Can permission="members.read">Visible</Can>
      </AuthorizationProvider>,
    );

    expect(screen.getByText("Visible")).toBeInTheDocument();
  });

  it("renders its fallback when the permission is absent", () => {
    render(
      <AuthorizationProvider authorization={authorization}>
        <Can permission="billing.read" fallback={<span>Unavailable</span>}>Visible</Can>
      </AuthorizationProvider>,
    );

    expect(screen.queryByText("Visible")).not.toBeInTheDocument();
    expect(screen.getByText("Unavailable")).toBeInTheDocument();
  });
});
