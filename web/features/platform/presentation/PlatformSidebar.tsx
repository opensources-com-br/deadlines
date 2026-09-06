"use client";

import Image from "next/image";
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
  { label: "Organization", href: "#organization", icon: Building2 },
  { label: "Plans", href: "#plans", icon: CreditCard },
];

const managementNavigation = [
  { label: "Team", href: "#team", icon: UsersRound },
  { label: "Access control", href: "#access-control", icon: Shield },
];

const securityNavigation = [
  { label: "Security", href: "#security", icon: History },
];

type PlatformSidebarProps = {
  user: UserProfile;
};

export function PlatformSidebar({ user }: PlatformSidebarProps) {
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
              {workspaceNavigation.map((item, index) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton
                    isActive={index === 0}
                    tooltip={item.label}
                    render={<a href={item.href} />}
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
                  <SidebarMenuButton tooltip={item.label} render={<a href={item.href} />}>
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
                  <SidebarMenuButton tooltip={item.label} render={<a href={item.href} />}>
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
            <SidebarMenuButton tooltip="Your account" render={<a href="#account" />}>
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
