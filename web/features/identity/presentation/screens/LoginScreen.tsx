import { Button } from "@/components/ui/button";
import { Field, FieldGroup } from "@/components/ui/field";
import { AuthShell } from "@/features/identity/presentation/components/AuthShell";

export function LoginScreen() {
  return (
    <AuthShell title="Welcome to Deadlines" description="Continue to access your workspace.">
      <form>
        <FieldGroup>
          <Field>
            <Button className="w-full" variant="outline" type="button">
              Continue with Google
            </Button>
          </Field>
        </FieldGroup>
      </form>
    </AuthShell>
  );
}
