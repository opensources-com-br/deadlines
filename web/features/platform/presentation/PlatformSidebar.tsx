"use client";

import Image from "next/image";
import {
  Building2,
  CreditCard,
  History,
  KeyRound,
  Mail,
  Monitor,
  ShieldCheck,
  UserRound,
  Users,
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

const navigation = [
  { label: "Organization", href: "#organization", icon: Building2 },
  { label: "Plans", href: "#plans", icon: CreditCard },
  { label: "Members", href: "#members", icon: Users },
  { label: "Invitations", href: "#invitations", icon: Mail },
  { label: "Permissions", href: "#permissions", icon: KeyRound },
  { label: "Roles", href: "#roles", icon: ShieldCheck },
  { label: "Your account", href: "#account", icon: UserRound },
  { label: "Organization history", href: "#history", icon: History },
  { label: "Active sessions", href: "#sessions", icon: Monitor },
];

type PlatformSidebarProps = {
  user: UserProfile;
};

export function PlatformSidebar({ user }: PlatformSidebarProps) {
  return (
    <Sidebar variant="floating" collapsible="icon">
      <SidebarHeader className="p-3">
        <div className="flex h-8 items-center gap-2 px-2 text-sm font-semibold tracking-tight">
          <Image className="size-6 invert" src="/deadlines-mark.png" alt="Deadlines" width={24} height={24} priority />
          <span>Deadlines</span>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Workspace</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navigation.map((item, index) => (
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
      </SidebarContent>
      <SidebarFooter className="p-3">
        <div className="min-w-0 px-2 py-1 group-data-[collapsible=icon]:hidden">
          <p className="truncate text-sm font-medium">{user.profile.firstName} {user.profile.lastName}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
