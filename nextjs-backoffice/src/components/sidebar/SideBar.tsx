"use client";

import { Children, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { Box, Flex } from "../ui/layout";
import { cn } from "@/utils/cn";

interface SideBarProps extends React.HTMLAttributes<HTMLDivElement> {}
interface SideBarHeaderProps extends React.HTMLAttributes<HTMLDivElement> {}
interface SideBarContentProps extends React.HTMLAttributes<HTMLDivElement> {}
interface SideBarFooterProps extends React.HTMLAttributes<HTMLDivElement> {}
interface SideBarMenuProps extends React.HTMLAttributes<HTMLDivElement> {}
interface SideBarMenuItemProps
  extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
}

export function SideBar({ children }: SideBarProps) {
  return (
    <Flex
      direction="column"
      className="w-[19.5rem] h-screen border-r-2 py-6 relative"
    >
      <Flex direction="column" className="absolute top-0 bottom-0 w-full">
        {children}
      </Flex>
    </Flex>
  );
}

export function SideBarHeader({ children, className }: SideBarHeaderProps) {
  return <Box className={cn("p-4 h-16", className)}>{children}</Box>;
}

export function SideBarContent({ children, className }: SideBarContentProps) {
  return (
    <Box className="flex-grow relative">
      <Flex direction="column" className="absolute top-0 bottom-0 w-full">
        <Box className={cn("flex-grow px-4 overflow-y-scroll", className)}>
          <Box>{children}</Box>
        </Box>
      </Flex>
    </Box>
  );
}

export function SideBarFooter({ children }: SideBarFooterProps) {
  return (
    <Box className="h-fit px-4">
      <Box className="border-t-2 pt-1 pb-2">{children}</Box>
    </Box>
  );
}

export function SideBarMenu({ children, className }: SideBarMenuProps) {
  return <ul className={cn("space-y-2 font-medium", className)}>{children}</ul>;
}

export function SideBarMenuItem({
  children,
  href,
  className,
}: SideBarMenuItemProps) {
  return (
    <li>
      <Link href={href} legacyBehavior>
        <Flex
          align="center"
          gap="3"
          className={cn("p-2 rounded-lg hover:bg-on-action-hover-2", className)}
        >
          {children}
        </Flex>
      </Link>
    </li>
  );
}
