"use client";

import Image from "next/image";
import Link from "next/link";
import { Bell, CircleHelp, CircleUserRound, CreditCard, EllipsisVertical, LogOut, Search, Settings } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

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
import { billingEnabled } from "@/lib/features";

type PlatformSidebarProps = {
  activeItem?: PlatformNavigationItem;
  user: UserProfile;
  onSignOut: () => void;
  isSigningOut: boolean;
};

export type SettingsSection = "organization" | "plans" | "team" | "access-control" | "security" | "account" | "notifications";
export type PlatformNavigationItem = "settings" | SettingsSection;

function userInitials(user: UserProfile) {
  return `${user.profile.firstName[0] ?? ""}${user.profile.lastName[0] ?? ""}`.toUpperCase() || user.email.slice(0, 2).toUpperCase();
}

export function PlatformSidebar({ activeItem, user, onSignOut, isSigningOut }: PlatformSidebarProps) {
  const t = useTranslations("Sidebar");
  const { isMobile } = useSidebar();
  const name = `${user.profile.firstName} ${user.profile.lastName}`.trim() || user.email;

  return (
    <Sidebar variant="floating" collapsible="icon">
      <SidebarHeader className="px-4 pt-5 pb-4 group-data-[collapsible=icon]:p-3 group-data-[collapsible=icon]:pt-4">
        <Link
          href="/app"
          aria-label={t("home")}
          className="flex h-8 items-center justify-center rounded-md px-2 outline-hidden hover:bg-sidebar-accent focus-visible:ring-2 focus-visible:ring-sidebar-ring group-data-[collapsible=icon]:px-0"
        >
          <span className="flex h-8 w-12 items-center justify-center rounded-md bg-white p-1" aria-hidden="true">
            <Image className="size-full object-contain" src="/opensources-sidebar-mark.png" alt="" width={40} height={24} priority />
          </span>
        </Link>
      </SidebarHeader>
      <SidebarContent />
      <SidebarFooter className="gap-3 p-3">
        <SidebarGroup className="p-0">
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton isActive={activeItem === "settings"} tooltip={t("settings")} render={<Link href="/app/settings/organization" />}>
                <Settings />
                <span>{t("settings")}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                tooltip={t("help")}
                type="button"
                onClick={() => toast.info(t("helpSoon"))}
              >
                <CircleHelp />
                <span>{t("help")}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                tooltip={t("search")}
                type="button"
                onClick={() => toast.info(t("searchSoon"))}
              >
                <Search />
                <span>{t("search")}</span>
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
                <DropdownMenuGroup>
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
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuGroup>
                  <DropdownMenuItem render={<Link href="/app/settings/account" />}>
                    <CircleUserRound />
                    {t("account")}
                  </DropdownMenuItem>
                  {billingEnabled ? <DropdownMenuItem render={<Link href="/app/settings/plans" />}>
                    <CreditCard />
                    {t("plans")}
                  </DropdownMenuItem> : null}
                  <DropdownMenuItem render={<Link href="/app/settings/notifications" />}>
                    <Bell />
                    {t("notifications")}
                  </DropdownMenuItem>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem disabled={isSigningOut} onClick={onSignOut}>
                  <LogOut />
                  {isSigningOut ? t("signingOut") : t("logout")}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
