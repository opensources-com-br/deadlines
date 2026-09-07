"use client";

import Image from "next/image";
import Link from "next/link";
import { Settings } from "lucide-react";

import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
type PlatformSidebarProps = { activeItem: PlatformNavigationItem };

export type SettingsSection = "organization" | "plans" | "team" | "access-control" | "security" | "account";
export type PlatformNavigationItem = "settings" | SettingsSection;

export function PlatformSidebar({ activeItem }: PlatformSidebarProps) {
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
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton isActive={activeItem === "settings"} tooltip="Settings" render={<Link href="/app/settings" />}>
                <Settings />
                <span>Settings</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
