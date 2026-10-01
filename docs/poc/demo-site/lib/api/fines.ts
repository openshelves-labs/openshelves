import type { Fine, FineSummary, Payment } from "@/types";
import { fines, payments } from "@/mock";

const clone = <T,>(v: T): T => structuredClone(v);

export async function getFines(): Promise<Fine[]> {
  return clone(fines);
}

export async function getPayments(): Promise<Payment[]> {
  return clone(payments);
}

export async function getFineSummary(): Promise<FineSummary> {
  const outstanding = fines.filter((f) => f.status === "unpaid").reduce((s, f) => s + f.amount, 0);
  const settledTotal = payments.reduce((s, p) => s + p.amount, 0);
  return { outstanding, settledTotal, currency: "USD", standing: "good", borrowLimit: 25 };
}
