import { BellRing, Mail } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const preferences = [
  { title: "Security alerts", description: "Important sign-in and password activity.", icon: BellRing },
  { title: "Organization updates", description: "Invitations and changes to your access.", icon: Mail },
  { title: "Deadline reminders", description: "Upcoming and overdue work. Available when deadlines launch.", icon: BellRing },
];

export function NotificationsCard() {
  return <Card><CardHeader><CardTitle>Email notifications</CardTitle><CardDescription>Notification delivery is not active yet. These preferences will become configurable when notifications launch.</CardDescription></CardHeader><CardContent className="space-y-3">{preferences.map(({ title, description, icon: Icon }) => <div key={title} className="flex items-center gap-3 rounded-lg border p-4"><div className="rounded-md bg-muted p-2"><Icon className="size-4" /></div><div className="min-w-0 flex-1"><p className="text-sm font-medium">{title}</p><p className="mt-1 text-xs text-muted-foreground">{description}</p></div><span className="rounded-full border px-2.5 py-1 text-xs text-muted-foreground">Coming soon</span></div>)}</CardContent></Card>;
}
