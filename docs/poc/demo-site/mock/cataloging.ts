import type { CatalogingDraft } from "@/types";

const COVER_OPEN_LIBRARY =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuA-HQBFJoI9FaWqub30G5un-oZra5JWUryg8Heeu4zOzYyDtxzk9PJJbaPrfB_wnaHefuBNzUVpI1GeW1ZpxhGN6OfYeFbiVNcnprvI4PQ_L-dNeA2BQwMCOSzoYayqSyzA4hKK7YMfikfFloQVsgO6zF_c-1EaAFYwULxuKvltaEslJ3Vw1-0F4jYDQRR13G6kICLZ3F6YIDcjT76cbDR8ELC2b_KGt1o8wVhL_7bL_MajwfLRSu-5Zg";
const COVER_GOOGLE_BOOKS =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuDQ59JmaqfEnqvwsgGdXejCqU8oJgKUYv2k4MdBvaAWPP9LHZ3uHSPwqsFPui1F0OPU-YDGxJHb0uhB4CtM8Bc6i4duOC302H42iW20xdiKDhz15vWNgDGwE8PIjz5hyki6TcsJKJnfnC2MpQCo6WjyFxBVqAl1YfCTLgvn3_SdvSkVSM16ZnjiwJTVmRhGHrs8wKIRnixMu4EEKD9T2x21TwO5DEjIaXqeirUo-JETSRhtoz_l-4Hb6A";
const COVER_HARDCOVER =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCdgHHEaQxMaplL4Ld4X9tqk3lQigimP_dx_y4zPbgRuHbRJrmN_--JJOx5Ke26rx1hpv4Ve7DrUhYmNP8g_volwCNbOLxWoBWSJ4tXWDZdyTgh9th7lxVRCrg-I6QjQzF6DPt1qJuOHjQGSqNaTAbZOU4DBarrJ0HsVaFb3xEi9gBdQtq_-YQxQWIhLwnOtowdKby8859jYkrfnFJNNCYURlsuzzeLFqa2L6UOhwO8jeHibYNwy3ay7w";

export const catalogingDraft: CatalogingDraft = {
  identifier: "978-0-19-883492-2",
  defaultProviderId: "open-library",
  providers: [
    {
      id: "open-library",
      name: "Open Library",
      confidence: 98,
      badge: "Recommended",
      coverUrl: COVER_OPEN_LIBRARY,
      coverTone: "parchment",
      card: {
        title: "Ecosystem Resilience and Forest Canopy Dynamics",
        authors: "Prof. Julian Sterling, Dr. Elena Rostova",
        publisher: "Oxford University Press",
        yearLabel: "2021 (2nd Ed.)",
      },
      record: {
        title: "Ecosystem Resilience and Forest Canopy Dynamics",
        authors: "Prof. Julian Sterling; Dr. Elena Rostova",
        year: "2021",
        publisher: "Oxford University Press",
        edition: "2nd Edition",
        callNumber: "QH541.15.R45 S74 2021",
        ddc: "577.3",
        subjects: ["Forest ecology", "Ecosystem resilience", "Canopy biology", "Environmental changes"],
        abstract:
          "Synthesizing twenty-five years of arboreal flux tower data across boreal and temperate zones, this volume models microclimatic buffering and structural resilience within old-growth canopies under extreme meteorological variation. It incorporates Dublin Core cross-references for botanical bioclimatology research cohorts.",
      },
      pickers: {
        authors: 'Open Library: "Prof. Julian Sterling, Dr. Elena Rostova"',
        year: "Open Library: 2021 (Copyright)",
      },
    },
    {
      id: "google-books",
      name: "Google Books",
      confidence: 94,
      badge: "MARC Excerpt",
      coverUrl: COVER_GOOGLE_BOOKS,
      coverTone: "sage",
      card: {
        title: "Ecosystem Resilience & Forest Canopy Dynamics",
        authors: "Julian Sterling, E. Rostova",
        publisher: "OUP Academic",
        yearLabel: "2020 (Catalogued 2021)",
      },
      record: {
        title: "Ecosystem Resilience & Forest Canopy Dynamics",
        authors: "Julian Sterling; E. Rostova",
        year: "2020",
        publisher: "OUP Academic",
        edition: "2nd Edition",
        callNumber: "QH541.15.R45 S74 2020",
        ddc: "577.3",
        subjects: ["Forest ecology", "Canopy biology", "Climate resilience"],
        abstract:
          "Twenty-five years of flux tower observations across boreal and temperate zones are synthesised to model microclimatic buffering and structural resilience in old-growth canopies under extreme meteorological variation.",
      },
      pickers: {
        authors: 'Google Books: "Julian Sterling, E. Rostova"',
        year: "Google Books: 2020 (Pre-print)",
      },
    },
    {
      id: "hardcover",
      name: "Hardcover Community",
      confidence: 88,
      badge: "Crowdsourced",
      coverUrl: COVER_HARDCOVER,
      coverTone: "forest",
      card: {
        title: "Ecosystem Resilience in Canopy Ecology",
        authors: "J. Sterling",
        publisher: "Oxford Univ Press",
        yearLabel: "2021",
      },
      record: {
        title: "Ecosystem Resilience in Canopy Ecology",
        authors: "J. Sterling",
        year: "2021",
        publisher: "Oxford Univ Press",
        edition: "",
        callNumber: "QH541.15 .S74",
        ddc: "577.3",
        subjects: ["Canopy ecology", "Forest ecology"],
        abstract: "Community-submitted summary: a study of canopy-level resilience in temperate and boreal forests.",
      },
      pickers: {
        authors: 'Hardcover: "J. Sterling"',
        year: "Hardcover: 2021",
      },
    },
  ],
};
