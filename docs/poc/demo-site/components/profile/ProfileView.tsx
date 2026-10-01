"use client";

import Link from "next/link";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import Avatar from "@/components/Avatar";

function Field({ label, value, icon, numeric = false }: { label: string; value?: string; icon: string; numeric?: boolean }) {
  return (
    <div className="flex items-start gap-3 rounded-lg border border-border-warm bg-[#F3F1EC]/60 px-4 py-3">
      <Icon name={icon} className="mt-0.5 text-[20px] text-primary" />
      <div className="min-w-0">
        <div className="font-metadata-caps text-[10px] font-semibold uppercase tracking-wider text-outline">{label}</div>
        <div className={`mt-0.5 break-words font-body-md text-sm text-on-surface ${numeric ? "tabular-nums" : ""}`}>{value ?? "—"}</div>
      </div>
    </div>
  );
}

export default function ProfileView() {
  const { user } = useLibrary();
  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <div className="font-metadata-caps text-[11px] font-semibold uppercase tracking-wider text-outline">Account</div>
        <h1 className="mt-1 font-title-editorial text-3xl font-semibold text-on-surface">My Profile</h1>
      </div>

      <section className="rounded-xl border border-border-warm bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
          <Avatar name={user.name} initial={user.initial} src={user.avatarUrl} className="h-20 w-20 text-2xl" />
          <div className="min-w-0 flex-1">
            <h2 className="font-title-editorial text-2xl font-semibold text-on-surface">{user.name}</h2>
            <p className="mt-0.5 font-body-md text-sm text-on-surface-variant">{user.program ?? user.department}</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[#c6ebd3]/70 px-2.5 py-0.5 font-label-md text-xs font-medium text-[#2F5D45]">{user.accountType}</span>
              <span className="flex items-center gap-1 font-label-md text-xs text-[#2F5D45]">
                <Icon name="verified" filled className="text-[15px]" /> Membership active
              </span>
            </div>
          </div>
          <Link
            href="/sign-in"
            className="flex items-center gap-2 self-start rounded-lg border border-border-warm bg-white px-4 py-2 font-label-lg text-sm font-semibold text-on-surface transition-colors hover:bg-[#F3F1EC] sm:self-center"
          >
            <Icon name="logout" className="text-[18px]" />
            Sign out
          </Link>
        </div>
      </section>

      <section className="rounded-xl border border-border-warm bg-white p-6 shadow-sm">
        <h2 className="mb-4 font-title-editorial text-lg font-semibold text-on-surface">Account details</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Full name" value={user.name} icon="badge" />
          <Field label="Student ID" value={user.studentId} icon="tag" numeric />
          <Field label="Program" value={user.program} icon="school" />
          <Field label="Department" value={user.department} icon="apartment" />
          <Field label="Institutional email" value={user.email} icon="mail" />
          <Field label="Library card number" value={user.cardNumber} icon="id_card" numeric />
          <Field label="Account type" value={user.accountType} icon="workspace_premium" />
          <Field label="Home library" value="Central University Library" icon="local_library" />
        </div>
      </section>
    </div>
  );
}
