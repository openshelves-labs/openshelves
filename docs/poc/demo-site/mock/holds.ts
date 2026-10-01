import type { Hold } from "@/types";
import { currentUser } from "./users";

export const holds: Hold[] = [
  {
    id: "h-krebs",
    userId: currentUser.id,
    bookId: "bk-krebs",
    status: "ready",
    placedOn: "2024-10-18",
    queuePosition: 1,
    queueLength: 1,
    pickupLocation: "Central Library Circulation Desk",
    pickupDetail: "Main Floor, West Wing",
    heldUntil: "2024-10-29",
    pickupCode: "OS-4471",
  },
];
