"use client";

import Image from "next/image";
import Link from "next/link";
import { CircleHelp, CircleUserRound, CreditCard, EllipsisVertical, LogOut, Search, Settings, ShieldCheck } from "lucide-react";
import { toast } from "sonner";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import type { UserProfile } from "@/features/platform/domain/user-profile";

type PlatformSidebarProps = {
  activeItem: PlatformNavigationItem;
  user: UserProfile;
  onSignOut: () => void;
  onSettingsSelect: (section: SettingsSection) => void;
  isSigningOut: boolean;
};

export type SettingsSection = "organization" | "plans" | "team" | "access-control" | "security" | "account";
export type PlatformNavigationItem = "settings" | SettingsSection;

function userInitials(user: UserProfile) {
  return `${user.profile.firstName[0] ?? ""}${user.profile.lastName[0] ?? ""}`.toUpperCase() || user.email.slice(0, 2).toUpperCase();
}

export function PlatformSidebar({ activeItem, user, onSignOut, onSettingsSelect, isSigningOut }: PlatformSidebarProps) {
  const { isMobile } = useSidebar();
  const name = `${user.profile.firstName} ${user.profile.lastName}`.trim() || user.email;

  return (
    <Sidebar variant="floating" collapsible="icon">
      <SidebarHeader className="p-3">
        <div className="flex h-8 items-center gap-2 px-2 text-sm font-semibold tracking-tight group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0">
          <Image className="size-6 invert" src="/deadlines-mark.png" alt="Deadlines" width={24} height={24} priority />
          <span className="group-data-[collapsible=icon]:hidden">Deadlines</span>
        </div>
      </SidebarHeader>
      <SidebarContent />
      <SidebarFooter className="gap-3 p-3">
        <SidebarGroup className="p-0">
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton isActive={activeItem === "settings"} tooltip="Settings" render={<Link href="/app/settings" />}>
                <Settings />
                <span>Settings</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                tooltip="Get Help"
                type="button"
                onClick={() => toast.info("The help center is coming soon.")}
              >
                <CircleHelp />
                <span>Get Help</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                tooltip="Search"
                type="button"
                onClick={() => toast.info("Search is coming soon.")}
              >
                <Search />
                <span>Search</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
        <SidebarMenu>
          <SidebarMenuItem>
            <DropdownMenu>
              <DropdownMenuTrigger render={<SidebarMenuButton size="lg" className="aria-expanded:bg-sidebar-accent" />}>
                <Avatar className="rounded-lg">
                  <AvatarFallback className="rounded-lg">{userInitials(user)}</AvatarFallback>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:hidden">
                  <span className="truncate font-medium">{name}</span>
                  <span className="truncate text-xs text-muted-foreground">{user.email}</span>
                </div>
                <EllipsisVertical className="ml-auto size-4 group-data-[collapsible=icon]:hidden" />
              </DropdownMenuTrigger>
              <DropdownMenuContent className="min-w-56" side={isMobile ? "bottom" : "right"} align="end" sideOffset={4}>
                <DropdownMenuLabel className="p-1 font-normal">
                  <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                    <Avatar className="rounded-lg">
                      <AvatarFallback className="rounded-lg">{userInitials(user)}</AvatarFallback>
                    </Avatar>
                    <div className="grid flex-1 leading-tight">
                      <span className="truncate font-medium">{name}</span>
                      <span className="truncate text-xs text-muted-foreground">{user.email}</span>
                    </div>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuGroup>
                  <DropdownMenuItem onClick={() => onSettingsSelect("account")}>
                    <CircleUserRound />
                    Account
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => onSettingsSelect("plans")}>
                    <CreditCard />
                    Plans
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => onSettingsSelect("security")}>
                    <ShieldCheck />
                    Security
                  </DropdownMenuItem>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem disabled={isSigningOut} onClick={onSignOut}>
                  <LogOut />
                  {isSigningOut ? "Signing out..." : "Log out"}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
