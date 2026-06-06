# Repository Instructions

## PUSH Command

When the user sends exactly `PUSH`, commit and push the current project to the configured remote repository.

Workflow:
- Check `git status --short` first.
- If there are no changes, report that the working tree is clean and do not create a commit.
- Stage all current changes with `git add .`.
- Create one commit using an Angular-style Conventional Commit message.
- Use a concise message based on the changed files and intent, for example `feat: add audit workflow`, `fix: correct login guard`, `chore: update project config`, or `docs: update push instructions`.
- Push the current branch to its upstream remote. If no upstream is configured, push to `origin` with `git push -u origin <current-branch>`.
- If pushing fails because of network or authentication, report the exact failure and leave the local commit intact.
