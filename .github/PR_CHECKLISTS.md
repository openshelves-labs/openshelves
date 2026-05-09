# Pull Request Checklists

Please review these checklists before submitting your Pull Request to ensure a smooth review process. You do not need to copy these into your PR description, but you should verify that your PR complies with them.

## ✅ Pre-Submission Checklist

- The PR is linked to an existing issue approved by the maintainers
- Code follows project style guidelines and is well-documented
- Branch is up to date with the base branch
- All existing tests are passing and new tests are added as needed
- The PR is well-documented and includes user-friendly descriptions of the changes
- The PR is focused on a single feature or bug fix
- The PR is well-scoped and contains reasonable lines of code (expected to be less than 1000 lines)
- No unnecessary refactors, dependence, or code changes are introduced
- Flyway migration versioning is up to date
- The PR is labeled appropriately (e.g., bug, enhancement, documentation, etc.)
- Proper documentation is provided for all new features and enhancements

## 🤖 AI-Assisted Contribution Checklist

If any part of the PR was generated or assisted by AI tools, please ensure the following:
*Note: You are responsible for every line of code that you submit.*

- I have read and understand every line of this PR and can explain any part of it during review
- I personally ran the code and verified it works (not just trusted the AI's output)
- PR is scoped to a single logical change, not a dump of everything the AI suggested
- Tests validate actual behavior, not just coverage (AI-generated tests often assert nothing meaningful)
- No dead code, placeholder comments, `TODO`s, or unused scaffolding left behind by AI
- I did not submit refactors, style changes, or "improvements" the AI suggested beyond the scope of the issue
