# UI restore point

The interface from before the professional visual refresh is preserved in Git at:

- Branch: `codex/ui-backup-before-refresh-2026-08-13`
- Commit: `e769e07`

Before switching branches, commit or stash any current work. Then preview the original interface with:

```bash
git switch codex/ui-backup-before-refresh-2026-08-13
```

To restore only the frontend files onto another branch, use the backup branch as the source:

```bash
git restore --source=codex/ui-backup-before-refresh-2026-08-13 -- traveldbFrontend
```

The backup branch is local and should remain unchanged as the pre-refresh reference.
