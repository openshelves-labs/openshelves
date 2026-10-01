import type { AppNotification } from "@/types";
import { notifications } from "@/mock";

export async function getNotifications(): Promise<AppNotification[]> {
  return structuredClone(notifications);
}
