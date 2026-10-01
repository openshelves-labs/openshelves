import type { Branch, User } from "@/types";
import { branches, currentUser } from "@/mock";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getCurrentUser(): Promise<User> {
  return clone(currentUser);
}

export async function getBranches(): Promise<Branch[]> {
  return clone(branches);
}
