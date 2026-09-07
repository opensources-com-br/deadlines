"use client";

import Image from "next/image";
import Link from "next/link";
import {
  Building2,
  CreditCard,
  History,
  Shield,
  UserRound,
  UsersRound,
} from "lucide-react";

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import type { UserProfile } from "@/features/platform/domain/user-profile";

const workspaceNavigation = [
  { key: "organization", label: "Organization", href: "/app/organization", icon: Building2 },
  { key: "plans", label: "Plans", href: "/app/plans", icon: CreditCard },
];

const managementNavigation = [
  { key: "team", label: "Team", href: "/app/team", icon: UsersRound },
  { key: "access-control", label: "Access control", href: "/app/access-control", icon: Shield },
];

const securityNavigation = [
  { key: "security", label: "Security", href: "/app/security", icon: History },
];

type PlatformSidebarProps = {
  user: UserProfile;
  activeItem: PlatformNavigationItem;
};

export type PlatformNavigationItem = "organization" | "plans" | "team" | "access-control" | "security" | "account";

export function PlatformSidebar({ user, activeItem }: PlatformSidebarProps) {
  return (
    <Sidebar variant="floating" collapsible="icon">
      <SidebarHeader className="p-3">
        <div className="flex h-8 items-center gap-2 px-2 text-sm font-semibold tracking-tight group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0">
          <Image className="size-6 invert" src="/deadlines-mark.png" alt="Deadlines" width={24} height={24} priority />
          <span className="group-data-[collapsible=icon]:hidden">Deadlines</span>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Workspace</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {workspaceNavigation.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton
                    isActive={item.key === activeItem}
                    tooltip={item.label}
                    render={<Link href={item.href} />}
                  >
                    <item.icon />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>Management</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {managementNavigation.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton isActive={item.key === activeItem} tooltip={item.label} render={<Link href={item.href} />}>
                    <item.icon />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup>
          <SidebarGroupLabel>Security</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {securityNavigation.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton isActive={item.key === activeItem} tooltip={item.label} render={<Link href={item.href} />}>
                    <item.icon />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="p-3">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton isActive={activeItem === "account"} tooltip="Your account" render={<Link href="/app/account" />}>
              <UserRound />
              <span>Your account</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
        <div className="min-w-0 px-2 py-1 group-data-[collapsible=icon]:hidden">
          <p className="truncate text-sm font-medium">{user.profile.firstName} {user.profile.lastName}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
