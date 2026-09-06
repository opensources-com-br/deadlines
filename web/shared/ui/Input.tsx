import type { InputHTMLAttributes } from "react";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string;
};

export function Input({ id, label, hint, className = "", ...props }: InputProps) {
  return (
    <label className="grid gap-2 text-sm font-medium text-zinc-800" htmlFor={id}>
      <span>{label}</span>
      <input
        id={id}
        className={`h-11 rounded-lg border border-zinc-300 bg-white px-3 text-base font-normal text-zinc-950 outline-none transition placeholder:text-zinc-400 focus:border-zinc-950 focus:ring-2 focus:ring-zinc-950/10 ${className}`}
        {...props}
      />
      {hint ? <span className="text-xs font-normal text-zinc-500">{hint}</span> : null}
    </label>
  );
}
