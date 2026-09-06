import type { InputHTMLAttributes, ReactNode } from "react";

import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";

type AuthTextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string;
  action?: ReactNode;
};

export function AuthTextField({ id, label, hint, action, className, ...props }: AuthTextFieldProps) {
  return (
    <Field>
      <div className="flex items-center justify-between gap-4">
        <FieldLabel htmlFor={id}>
          {label}
        </FieldLabel>
        {action}
      </div>
      <Input id={id} className={className} {...props} />
      {hint ? <FieldDescription>{hint}</FieldDescription> : null}
    </Field>
  );
}
