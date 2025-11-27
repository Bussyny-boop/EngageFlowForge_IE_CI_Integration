# Commit Naming Conventions

We follow a **semantic, conventional commit style** to improve readability of history and enable automated changelog generation. Please adhere to these guidelines when committing code.

---

## 📐 Format

```
<type>(<scope>): <short summary>

<body>  (optional)

<footer>  (optional)
```

### Components

1. **`type`** – One of the allowed prefixes below (lowercase):
   - `feat` – New feature for the user
   - `fix` – Bug fix
   - `refactor` – Code changes that neither fix a bug nor add a feature
   - `docs` – Documentation only
   - `style` – Formatting, missing semicolons, whitespace, etc. (not CSS)
   - `test` – Adding or fixing tests
   - `chore` – Maintenance tasks (build tooling, dependencies, CI/CD)
   - `perf` – Performance improvements
   - `ci` – CI/CD configuration changes

2. **`scope`** – Optional, lowercase noun describing the area of the codebase affected:
   - Examples: `sidebar`, `theme`, `ci`, `parser`, `ui`, `deps`

3. **`short summary`** – Imperative mood, present tense, ≤ 50 characters:
   - ✅ "add Vocera-style accordion with icons"
   - ❌ "Added vocera style accordion."
   - Capitalize only the first letter
   - Do not end with a period

---

## ✅ Examples

### Feature
```
feat(sidebar): add Vocera-style accordion with icons
```

### Bug Fix
```
fix(theme): correct gradient colors in light mode
```

### Refactoring
```
refactor(controller): extract theme toggling into helper method
```

### Documentation
```
docs(readme): update installation instructions
```

### Styling
```
style(css): adjust sidebar gradients and padding
```

### Testing
```
test(parser): add unit tests for JSON export
```

### Chores
```
chore(deps): bump JavaFX to 21.0.3
```

### Performance
```
perf(ui): optimize accordion expand/collapse animation
```

---

## 📝 Body (Optional)

Use the body to provide additional context, motivation, or contrast with previous behavior:

- Wrap lines at **72 characters**
- Use bullet points for multiple changes
- Explain **why** the change was made, not just **what** changed

**Example:**
```
feat(sidebar): add Vocera-style accordion with icons

- Replaced flat button list with collapsible accordion sections
- Added custom teal icons for LOAD DATA, VIEWS, ACTIONS, EXPORT, TOOLS
- Improved UX by grouping related actions under expandable headers
- Matches Vocera Platform Server UI design guidelines
```

---

## 📌 Footer (Optional)

Use the footer for:

1. **Breaking changes:**
   ```
   BREAKING CHANGE: dropping Java 8 support, now requires Java 17+
   ```

2. **Issue references:**
   ```
   Fixes #123
   Closes #456
   Related to #789
   ```

3. **Co-authors:**
   ```
   Co-authored-by: Jane Doe <jane@example.com>
   ```

---

## 🚫 What to Avoid

- ❌ Vague summaries: `fix stuff`, `update code`, `changes`
- ❌ Past tense: `Added feature`, `Fixed bug`
- ❌ Periods at the end of the summary
- ❌ Overly long summaries (>50 chars)
- ❌ Mixing multiple unrelated changes in one commit

---

## 🔗 Best Practices

1. **One logical change per commit** – If fixing two bugs, make two commits.
2. **Commit often** – Small, focused commits are easier to review and revert.
3. **Test before committing** – Ensure the code compiles and tests pass.
4. **Write for humans** – Your future self (and teammates) will thank you.

---

## 🛠️ Tools

### Pre-commit Hooks
Use [Commitlint](https://commitlint.js.org/) or [Husky](https://typicode.github.io/husky/) to enforce these conventions automatically.

### Example `.commitlintrc.json`
```json
{
  "extends": ["@commitlint/config-conventional"],
  "rules": {
    "type-enum": [
      2,
      "always",
      ["feat", "fix", "refactor", "docs", "style", "test", "chore", "perf", "ci"]
    ],
    "subject-case": [2, "always", "sentence-case"]
  }
}
```

---

## 📚 References

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
- [Semantic Versioning](https://semver.org/)

---

**Following these conventions helps maintain a clean, searchable history and enables automated tooling!** 🚀
