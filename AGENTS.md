# Repository instructions

## Windows terminal

- Use PowerShell 7, preferably `C:\Program Files\PowerShell\7\pwsh.exe`, with `login = false`.
- Use Windows PowerShell 5.1 only if PowerShell 7 is unavailable.

## Commit messages

- Format commit subjects as `[ModuleName] type: description`.
- Use the module's directory name as the scope: changes to `NaturesAura/` use `[NaturesAura]`.
- Use the corresponding module name for future mod subprojects. Split independent changes across modules into separate commits.
- Use `[Repo]` for changes that only affect shared repository infrastructure or policies.
- Choose an appropriate conventional type, such as `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `build`, `ci`, `style`, `chore`, or `revert`.
- Keep the subject concise and describe the resulting change. Do not include Markdown asterisks around the scope.
- Examples: `[NaturesAura] feat: add ore condensation chamber`, `[NaturesAura] fix: preserve machine inventory`, `[Repo] ci: update build workflow`.
