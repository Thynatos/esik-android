# Agent Instructions

Read `.github/copilot-instructions.md`, `docs/PRODUCT_SPEC.md`, and `docs/DATA_SCHEMA.md` before editing.

Work slice by slice. Inspect actual files before assuming paths or APIs. Keep the product to exactly four screens. Preserve the mock AI path until the real API path is proven, so UI work never blocks on networking.

Do not commit secrets. Do not add medical diagnosis language. Do not let AI decide thresholds. Run crisis filtering before any remote AI request. Validate AI output before display and fall back to deterministic neutral copy on every network or parse error.

For Android core changes, test on a physical device and report the Android version/OEM. For pure logic, add or update unit tests.
