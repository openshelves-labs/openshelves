import type { Book, Contributor } from "@/types";
import { COVERS } from "./covers";

/** Screen-specific books for the home screen (its "New Arrivals" tiles). No accessionDate on purpose. */
const author = (name: string, family: string, given: string): Contributor => ({ name, family, given, role: "author" });

/** Order of the five Home tiles (Krebs lives in books-core). */
export const homeHighlightIds = ["bk-krebs", "bk-home-anthropocene", "bk-home-ethics", "bk-home-boreal", "bk-home-paradigms"];

export const homeBooks: Book[] = [
  {
    id: "bk-home-anthropocene",
    title: "Anthropocene Forests & Soil Moisture",
    contributors: [author("H. Lindqvist et al.", "Lindqvist", "H.")],
    publisher: "Nordic Academic Press",
    publicationYear: 2024,
    callNumber: "QK938 .F6 A58 2024",
    language: "English",
    resourceType: "monograph",
    subjects: ["Forest Ecology", "Soil Science"],
    coverUrl: COVERS.chapin,
    coverTone: "parchment",
    availabilityStatus: "available",
    availabilityLabel: "Available on Shelf",
    copies: [],
  },
  {
    id: "bk-home-ethics",
    title: "Ethical Conservation Frameworks",
    contributors: [author("Prof. A. MacIntyre", "MacIntyre", "A.")],
    publisher: "Riverbend University Press",
    publicationYear: 2024,
    callNumber: "QH75 .E84 2024",
    language: "English",
    resourceType: "monograph",
    subjects: ["Conservation Ethics", "Environmental Philosophy"],
    coverUrl: COVERS.carson,
    coverTone: "parchment",
    availabilityStatus: "available",
    availabilityLabel: "Available on Shelf",
    copies: [],
  },
  {
    id: "bk-home-boreal",
    title: "Boreal Peatland Fluxes",
    contributors: [author("T. K. Westergaard", "Westergaard", "T. K.")],
    publisher: "Arctic Earth Sciences Press",
    publicationYear: 2024,
    callNumber: "GB621 .W47 2024",
    language: "English",
    resourceType: "monograph",
    subjects: ["Peatlands", "Carbon Cycling"],
    coverTone: "sage",
    availabilityStatus: "available",
    availabilityLabel: "Available on Shelf",
    copies: [],
  },
  {
    id: "bk-home-paradigms",
    title: "Scientific Paradigms Vol. 4",
    contributors: [author("Thomas S. Kuhn", "Kuhn", "Thomas S.")],
    publisher: "University of Chicago Press",
    publicationYear: 2023,
    callNumber: "Q175 .K95 v.4",
    language: "English",
    resourceType: "monograph",
    subjects: ["Philosophy of Science"],
    coverUrl: COVERS.kuhn,
    coverTone: "forest",
    availabilityStatus: "available",
    availabilityLabel: "Available on Shelf",
    copies: [],
  },
];
