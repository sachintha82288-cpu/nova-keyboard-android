# apk-analysis

Dropping a `run.json` here triggers `.github/workflows/apk-analysis.yml`, which downloads the
published `v1.0.0` release artefacts, runs `scripts/apk_analysis.py` and commits the resulting
`apk-report.txt` / `apk-report.json` back to the branch.

The shipping binaries themselves are never committed (see `.gitignore`); only the reports are.
