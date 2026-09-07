"use client"

import * as React from "react"
import { Tabs as TabsPrimitive } from "@base-ui/react/tabs"
import { cn } from "cn"

function Tabs({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Root>) {
  return <TabsPrimitive.Root data-slot="tabs" className={cn("flex flex-col gap-5", className)} {...props} />
}

function TabsList({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) {
  return <TabsPrimitive.List data-slot="tabs-list" className={cn("flex w-fit max-w-full items-center gap-1 overflow-x-auto rounded-xl border bg-card/60 p-1", className)} {...props} />
}

function TabsTrigger({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Tab>) {
  return <TabsPrimitive.Tab data-slot="tabs-trigger" className={cn("shrink-0 rounded-[9px] px-4 py-2 text-sm font-semibold text-muted-foreground outline-none transition-[background-color,color,box-shadow] duration-150 hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring data-[active]:bg-secondary data-[active]:text-secondary-foreground data-[active]:shadow-sm", className)} {...props} />
}

export { Tabs, TabsList, TabsTrigger }
