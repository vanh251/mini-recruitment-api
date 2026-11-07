# Repository Guidelines

## Project Structure & Module Organization

This workspace currently has no application source, tests, assets, dependency manifests, or Git metadata. Establish the layout when adding the initial implementation. Suggested directories are `src/` for application code, `tests/` for automated tests, and `assets/` for static resources. Group related modules by feature and document the chosen layout in `README.md`.

## Build, Test, and Development Commands

No build, test, or development commands are configured yet. Do not assume commands such as `npm install` or `npm test` are available until a matching manifest and scripts exist.

- `Get-ChildItem -Force`: inspect the workspace, including hidden files, in PowerShell.
- `Get-Content README.md`: read setup instructions once the README exists.

When introducing tooling, document exact installation, local development, build, and test commands alongside required runtime versions.

## Coding Style & Naming Conventions

No language, formatter, or linter has been selected. Use consistent indentation and descriptive names within each language introduced. Follow its standard naming conventions; avoid mixing filename styles within a module. Configure a formatter and linter with the initial implementation, and keep their configuration in version control. Keep modules focused and avoid unrelated formatting changes.

## Testing Guidelines

No test framework or coverage threshold is configured. Add tests with new behavior and bug fixes once a framework is chosen. Use descriptive test names that identify the behavior and expected outcome, and follow the framework's file naming convention. Document how to run the complete suite and individual tests. Until automation exists, include reproducible manual verification steps in pull requests.

## Commit & Pull Request Guidelines

No Git history is available to establish existing commit conventions. Use concise, imperative subjects, such as `Add initial application scaffold`, and keep commits focused on one change.

Pull requests should explain the purpose, summarize changes, link relevant issues, and list verification performed. Include screenshots for visible interface changes and setup notes for new dependencies or configuration.

## Security & Configuration

Keep credentials and local configuration out of version control. Provide placeholder values in an example configuration file and document required variables when configuration is introduced.
