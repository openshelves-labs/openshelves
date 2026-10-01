"use client";

import { useEffect, useRef, useState } from "react";
import type { CatalogProviderId, CatalogRecord, CatalogingDraft } from "@/types";
import { useLibrary } from "@/lib/store";
import Icon from "@/components/Icon";
import IdentifierLookup, { IDENTIFIER_TABS, type IdentifierTabId } from "./IdentifierLookup";
import ProviderMatches from "./ProviderMatches";
import MetadataForm, { type ResolvedState } from "./MetadataForm";
import CitationStrip, { type CiteFormat } from "./CitationStrip";
import ActionBar, { type SaveState } from "./ActionBar";
import StepIndicator from "./StepIndicator";

const LOOKUP_MS = 900;

export default function AddResourceView({ draft }: { draft: CatalogingDraft }) {
  const { user } = useLibrary();
  const defaultProvider = draft.providers.find((p) => p.id === draft.defaultProviderId) ?? draft.providers[0];
  const recommended = draft.providers.find((p) => p.id === "open-library") ?? draft.providers[0];

  const initialIds = Object.fromEntries(IDENTIFIER_TABS.map((t) => [t.id, t.id === "isbn" ? draft.identifier : ""])) as Record<
    IdentifierTabId,
    string
  >;

  const [tab, setTab] = useState<IdentifierTabId>("isbn");
  const [identifiers, setIdentifiers] = useState(initialIds);
  const [format, setFormat] = useState("general");
  const [loading, setLoading] = useState(false);
  const [providerId, setProviderId] = useState<CatalogProviderId>(defaultProvider.id);
  const [record, setRecord] = useState<CatalogRecord>(defaultProvider.record);
  const [resolved, setResolved] = useState<ResolvedState>({ authors: false, year: false });
  const [citeFormat, setCiteFormat] = useState<CiteFormat>("apa");
  const [saveState, setSaveState] = useState<SaveState>("auto");
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);

  const edit = (patch: Partial<CatalogRecord>) => {
    setRecord((r) => ({ ...r, ...patch }));
    setSaveState((s) => (s === "saved" ? "dirty" : s));
  };

  const rerun = () => {
    if (loading) return;
    setLoading(true);
    timer.current = setTimeout(() => setLoading(false), LOOKUP_MS);
  };

  const selectProvider = (id: CatalogProviderId) => {
    const p = draft.providers.find((x) => x.id === id);
    if (!p) return;
    setProviderId(id);
    setRecord(p.record);
    setResolved({ authors: false, year: false });
    setSaveState((s) => (s === "saved" ? "dirty" : s));
  };

  const acceptAll = () => {
    edit({ authors: recommended.record.authors, year: recommended.record.year });
    setResolved({ authors: true, year: true });
  };

  const reset = () => {
    if (timer.current) clearTimeout(timer.current);
    setLoading(false);
    setTab("isbn");
    setIdentifiers(initialIds);
    setFormat("general");
    setProviderId(defaultProvider.id);
    setRecord(defaultProvider.record);
    setResolved({ authors: false, year: false });
    setCiteFormat("apa");
    setSaveState("auto");
  };

  const identifier = identifiers[tab];
  const tabLabel = IDENTIFIER_TABS.find((t) => t.id === tab)?.short ?? "";

  return (
    <main className="relative mx-auto min-h-screen w-full max-w-[1600px] bg-surface px-8 py-margin">
      <div className="flex w-full max-w-6xl flex-col space-y-space-xl">
        <header className="flex flex-col justify-between gap-space-md md:flex-row md:items-end">
          <div className="max-w-2xl space-y-space-xs">
            <nav aria-label="Catalog hierarchy" className="flex items-center gap-2 font-metadata-caps text-metadata-caps uppercase tracking-wider text-outline">
              <span>Catalog Administration</span>
              <span className="text-outline-variant">/</span>
              <span>Accessions &amp; Ingestion</span>
              <span className="text-outline-variant">/</span>
              <span className="font-semibold text-primary">Add Resource</span>
            </nav>
            <h1 className="font-headline-lg text-headline-lg tracking-tight text-on-surface">Catalog New Resource</h1>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Ingest bibliographic records from external metadata providers or mint new accession records with automated authority
              verification.
            </p>
          </div>
          <div className="flex shrink-0 items-center gap-space-sm rounded-lg bg-surface-container px-space-md py-space-sm">
            <Icon name="badge" className="text-[20px] text-primary" />
            <div className="flex flex-col">
              <span className="font-label-md text-label-md font-semibold text-on-surface">{user.name}</span>
              <span className="font-metadata-caps text-metadata-caps text-outline">{user.department}</span>
            </div>
          </div>
        </header>

        <StepIndicator />

        <IdentifierLookup
          tab={tab}
          onTab={setTab}
          identifier={identifier}
          onIdentifier={(v) => setIdentifiers((m) => ({ ...m, [tab]: v }))}
          format={format}
          onFormat={setFormat}
          loading={loading}
          onRerun={rerun}
        />

        <ProviderMatches
          providers={draft.providers}
          selectedId={providerId}
          current={record}
          loading={loading}
          querySummary={identifier.trim() ? `${tabLabel} ${identifier.trim()}` : ""}
          onSelect={selectProvider}
        />

        <MetadataForm
          record={record}
          providers={draft.providers}
          resolved={resolved}
          onEdit={edit}
          onResolve={(k) => setResolved((r) => ({ ...r, [k]: true }))}
          onAcceptAll={acceptAll}
        />

        <CitationStrip record={record} isbn={tab === "isbn" ? identifier.trim() : ""} format={citeFormat} onFormat={setCiteFormat} />

        <ActionBar saveState={saveState} onSave={() => setSaveState("saved")} onReset={reset} />
      </div>
    </main>
  );
}
