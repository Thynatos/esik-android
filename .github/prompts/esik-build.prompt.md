---
agent: 'agent'
description: 'Implement the Eşik Android hackathon prototype slice by slice'
---

Implement the Eşik Android prototype in this repository.

Before editing:

1. Read `COPILOT_PROMPT.md` in full; it is the canonical executor prompt.
2. Read `.github/copilot-instructions.md`, `docs/PRODUCT_SPEC.md`, `docs/DATA_SCHEMA.md`, and `docs/IMPLEMENTATION_HANDOFF.md`.
3. Inspect the actual repository and compare it with the plan. Report any stale path, API, version, or behavior before changing code.

Then execute one vertical slice at a time. Preserve the deterministic mock and offline demo, keep exactly four product screens, run the crisis gate before every remote request, validate every generated output, never commit credentials, and validate each meaningful slice before continuing.

The final response must list files changed, slice-by-slice results, commands and physical-device checks performed, mismatches found, unresolved risks, and whether the mock/offline path still works.
