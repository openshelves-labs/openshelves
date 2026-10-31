# 📖 OpenShelves Docs

*Every library keeps a few books about itself.*

[← Project README](../README.md)

Everything written down about OpenShelves lives here: what we're building and why, how it's designed, and the sketches and experiments that got us here. If you're about to change something, there's probably a page below that explains how it's meant to work.

**Contents**

1. [🧭 Start here](#-start-here)
2. [📐 Design](#-design)
3. [🧪 Prototypes and experiments](#-prototypes-and-experiments)
4. [🗂️ Reference material](#️-reference-material)
5. [🖼️ Assets](#️-assets)

---

## 🧭 Start here

New to the project? Read these two first; they explain who OpenShelves is for and what it has to do.

| | Doc | What's inside |
|---|---|---|
| 📋 | [Product requirements](Digital%20Library%20Management%20Platform.pdf) | The original brief: the people who use the library (students, faculty, librarians), the core ideas (resources, loans, holds) and what the platform must do |
| 🧭 | [User journey maps](User%20Journey%20Maps.pdf) | How patrons and librarians move through the product, step by step |

---

## 📐 Design

How the pieces are meant to fit together. Keep these in step with the code: if you change how something works, update its page in the same PR.

| | Doc | What's inside |
|---|---|---|
| 🔌 | [API design](api-design/README.md) | Every endpoint, grouped by feature, plus the conventions they all share |
| 🎨 | [Design system](Design%20System.md) | Colours, typography and components behind the web app |
| 🖥️ | [Frontend architecture](Frontend%20Architecture.md) | How the Next.js app is structured, loads data and handles sign-in |

---

## 🧪 Prototypes and experiments

Early work, kept for reference. Useful for seeing where an idea came from, but not a description of how things work today.

| | Where | What's inside |
|---|---|---|
| 🧪 | [Demo site](poc/demo-site/README.md) | The clickable prototype the web app is being rebuilt from |
| ✏️ | [`poc/sample drafts/`](poc/sample%20drafts) | Early page mock-ups: the home page and the librarian's metadata review screens |
| 🤖 | [`prompts/metadata_resolver/`](prompts/metadata_resolver) | An experiment in using an AI model to settle conflicting book metadata from different providers |

---

## 🗂️ Reference material

| | Where | What's inside |
|---|---|---|
| 📚 | [`openlibrary/`](openlibrary) | JSON schemas for Open Library's author and edition data dumps, worked out from the dumps themselves |

---

## 🖼️ Assets

[`assets/`](assets) holds the images used across the READMEs and docs: the banners, the architecture diagram and the colour palette. They're hand-made SVGs, so they can be edited in any text editor.

---

<div align="center">

*A library is only as good as its index.*

[← Back to the project README](../README.md)

</div>
