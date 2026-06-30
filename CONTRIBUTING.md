# Contributing to OpenShelves

> **Modernizing the university library experience — together.**

Welcome, and thank you for your interest in contributing to OpenShelves! Whether you're fixing a bug, proposing a feature, improving documentation, or asking a question — you are part of something meaningful. We're building an open discovery platform for academic libraries, and every contribution matters.

Please read this guide before submitting your first contribution. It covers how to contribute, what to expect, and the agreements that protect both you and the project.

---

## Table of Contents

- [Our Philosophy](#our-philosophy)
- [Contributor License Agreement](#contributor-license-agreement)
- [How to Contribute](#how-to-contribute)
- [Code of Conduct](#code-of-conduct)
- [Attribution and Credit](#attribution-and-credit)
- [Governance and Decisions](#governance-and-decisions)
- [License](#license)

---

## Our Philosophy

OpenShelves is built on the belief that library technology should be **open, accessible, and community-driven**. We actively welcome contributions from developers, librarians, designers, students, and anyone who shares that vision.

At the same time, we have a responsibility to keep the project **legally sound, sustainably maintained, and protected for the long term**. The policies in this guide exist for exactly that purpose — not to create barriers, but to create a stable foundation for everyone.

---

## Contributor License Agreement

**Before your first contribution is merged, you must agree to the [OpenShelves Contributor License Agreement (CLA)](./CLA.md).**

### What the CLA does

The CLA is a legal agreement between you and OpenShelves Labs. Here is a plain-language summary of what it means:

| What | Details |
|---|---|
| **Your credit is guaranteed** | Your contributions are attributed to you permanently in the project's history. License changes do not erase your authorship. |
| **Your code outside this project is yours** | The CLA applies only to code as it exists within OpenShelves. You are free to use, license, or commercialize the same code independently, under any terms you choose. |
| **The Core Team controls project-level licensing** | Decisions about the project's license, monetization, or business policy rest with OpenShelves Labs. By contributing, you agree that such changes don't require your individual sign-off. |
| **Attribution is required on forks** | Anyone redistributing or forking this project must explicitly attribute OpenShelves as the original source. Re-branding without attribution is not permitted. |
| **Patent rights** | You grant a patent license covering your contributions within this project. You retain your patent rights over independent uses of the same code. |

**→ [Read the full CLA here](./CLA.md)**

### How to sign the CLA

Acceptance is done directly in your pull request. When you open a PR, the template will include the following checkbox at the bottom:

> I agree to the OpenShelves Contributor License Agreement (CLA).

**Check that box before submitting.** An automated check runs on every PR and will set a failing status if the box is unchecked, blocking the PR from being merged until it is ticked.

---

## How to Contribute

### Reporting Issues

If you've found a bug or have a feature request, please [open an issue](https://github.com/openshelves-labs/openshelves/issues). Before filing, check whether a similar issue already exists.

When reporting a bug, please include:
- A clear description of the problem
- Steps to reproduce
- Expected vs. actual behavior
- Your environment (OS, Java version, etc.)

### Suggesting Features

Have an idea? We'd love to hear it. Open a [Discussion](https://github.com/openshelves-labs/openshelves/discussions) to share your proposal before building it. This avoids duplicated effort and helps ensure your contribution aligns with the project's direction.

### Submitting Code

> [!IMPORTANT]
> **Checklists First!**
> Before you open your pull request, please review the **[PR Checklists](./.github/PR_CHECKLISTS.md)** to ensure your code meets our project standards and AI-assistance rules.

1. **Fork the repository** and create a branch from `develop`.
2. **Write your code.** Follow the existing code style and conventions.
3. **Write tests** where applicable.
4. **Commit your changes** with a clear, descriptive commit message.
5. **Open a pull request** against the `develop` branch.
6. **Check the CLA and Checklist boxes** in the PR description to confirm you've met the requirements.
7. **Respond to review feedback** — a maintainer will review your PR and may request changes.

Pull requests that pass review and CI will be merged by a maintainer. We aim to review PRs within a reasonable timeframe, though this may vary depending on the complexity and the team's availability.

### Documentation

Good documentation is as valuable as good code. Improvements to READMEs, inline comments, API docs, or user guides are always welcome.

---

## Code of Conduct

We are committed to a welcoming and respectful community. All contributors are expected to:

- Be kind and constructive in discussions
- Respect differing viewpoints and experiences
- Accept feedback gracefully
- Prioritize the project's health over individual preferences

Harassment, discrimination, or disrespectful behavior of any kind will not be tolerated. Violations may result in removal from the project. If you experience or witness unacceptable behavior, please reach out to the Core Team via a private message or email.

---

## Attribution and Credit

Every contributor's work is preserved in the project's git history and is considered a permanent part of the project's record. Significant contributions may also be recognized in a `CONTRIBUTORS` file or release notes.

We do not erase or suppress contributor history. If you believe your contribution has been incorrectly attributed or is missing attribution, please open an issue and we will address it promptly.

---

## Governance and Decisions

OpenShelves Labs values community input and welcomes open discussion on project direction, feature priorities, and policies. All major discussions happen publicly in [GitHub Discussions](https://github.com/openshelves-labs/openshelves/discussions).

That said, **final decisions about the project — including technical direction, licensing, and business policy — rest with the OpenShelves Labs Core Team.** This structure is intentional: it protects the project from governance deadlocks and licensing conflicts that can destabilize open-source projects over time.

For a full description of roles, decision-making authority, and current Core Team membership, see [GOVERNANCE.md](./GOVERNANCE.md). For non-urgent changes, the Core Team aims to announce decisions publicly before they take effect. Urgent changes required for legal, security, or compliance reasons may take effect without advance notice.

We believe this is the right balance between openness and sustainability. If you disagree with a decision, we encourage you to raise it respectfully in a Discussion. We listen.

---

## License

OpenShelves is licensed under the **Business Source License 1.1 (BUSL)**, converting to Apache License 2.0 on a rolling basis per the Change Date defined in the `LICENSE` file. By contributing, you agree to the terms described in the [Contributor License Agreement](./CLA.md).

You are free to use, fork, and build upon OpenShelves in your own projects under the BUSL terms — provided you respect the Additional Use Grant limits and include proper attribution as described in the CLA.

---

*Thank you for helping build OpenShelves. Every line of code, every bug report, and every suggestion makes a difference.*

— The OpenShelves Labs Core Team
