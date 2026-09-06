import type { InputHTMLAttributes, ReactNode } from "react";

import { Input } from "@/components/ui/input";

type AuthTextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string;
  action?: ReactNode;
};

export function AuthTextField({ id, label, hint, action, className, ...props }: AuthTextFieldProps) {
  return (
    <div className="grid gap-2">
      <div className="flex items-center justify-between gap-4">
        <label className="text-sm font-medium text-foreground" htmlFor={id}>
          {label}
        </label>
        {action}
      </div>
      <Input id={id} className={className} {...props} />
      {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
    </div>
  );
}
