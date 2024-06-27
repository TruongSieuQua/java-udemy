"use client";

import React, { forwardRef } from "react";
import { VariantProps, tv } from "tailwind-variants";

const input = tv({
  base: "input",
  variants: {
    size: {
      xs: "input-xs",
      sm: "input-sm",
      md: "input-md",
      lg: "input-lg",
    },
    border: {
      true: "input-bordered",
      false: "",
    },
    color: {
      default: "",
      primary: "input-primary",
      secondary: "input-secondary",
      accent: "input-accent",
      success: "input-success",
      info: "input-info",
      warning: "input-warning",
      error: "input-error",
      ghost: "input-ghost",
    },
    width: {
      fit: "w-fit",
      full: "w-full",
    },
  },
  defaultVariants: {
    size: "md",
    border: true,
    color: "default",
    width: "full",
  },
});
const inputWithText = tv({
	extend: input,
	base: "input input-bordered flex items-center gap-2"
})
const inputWithIcon = tv({
	extend: input,
	base: "input input-bordered flex items-center gap-2"
})

type InputVariantsType = VariantProps<typeof input>;
interface InputProps
  extends Omit<
      React.InputHTMLAttributes<HTMLInputElement>,
      keyof InputVariantsType
    >,
    InputVariantsType {}

/*
 * Input is a component that renders an input element.
 * It accepts all the props that an input element accepts.
 */
const Input = forwardRef(
  ({ size, color, width, className, ...rest }: InputProps, ref) => {
    return (
      <input
        className={input({ size, color, width, className })}
        {...rest}
      />
    );
  },
);

/*
 * InputTextLabel is a component that renders a label element inside input.
 */
interface InputTextLabelProps extends InputProps {
  label: string;
}

export function InputTextLabel({
  size,
  color,
  width,
  className,
  children,
  label,
  ...rest
}: InputTextLabelProps) {
  return (
    <label
      className={inputWithText({ size, color, width, className })}
    >
      {label}
      <input className="grow" {...rest} />
    </label>
  );
}

/*
* InputIcon is a component that renders an icon inside input.
*/
interface InputIconProps extends InputProps {
	icon: React.ReactNode;
	iconPosition?: "left" | "right";
}
export function InputIcon({
  size,
  color,
  width,
  className,
  children,
	icon,
  iconPosition="left",
  ...rest
}: InputIconProps) {
  return (
    <label
      className={inputWithIcon({ size, color, width, className })}
    >
      {iconPosition === "left" && icon}
      <input className="grow" {...rest} />
			{iconPosition === "right" && icon}
    </label>
  );
}
Input.displayName = "Input";

export { Input };
