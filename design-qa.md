# Design QA — Settings tabs

- Source visual truth: `/var/folders/15/bsnxdb1n0r9446hfq1kl71t80000gn/T/codex-clipboard-22566d89-5049-4a3a-b3a6-e0f8a44795c2.png`
- Implementation URL attempted: `http://localhost:3001/app/organization`
- Intended viewport: desktop web app; source is 453 × 103 px
- Implementation screenshot: unavailable
- State: Settings with an active primary and secondary tab

## Full-view comparison evidence

The source image was opened and inspected. It specifies a compact dark tab capsule with a 12 px outer radius, fine neutral border, 4 px inner gap/padding, muted inactive labels, and a filled rounded active state.

The browser could not reach the locally launched Next.js server (`ERR_CONNECTION_REFUSED`), so no browser-rendered implementation screenshot was available for a faithful comparison.

## Focused region comparison evidence

Blocked: the settings tabs could not be captured in the running application. The source region is small and requires a same-state rendered crop to validate its tab height, radius, spacing, and selected state.

## Required fidelity surfaces

- Fonts and typography: implementation uses the existing Geist-based application typography; browser-rendered weights and antialiasing remain unverified.
- Spacing and layout rhythm: code sets a compact capsule, 4 px internal gap, and 9 px selected-tab radius; visual comparison remains blocked.
- Colors and visual tokens: code maps the source’s dark surface to existing `card`, `border`, `secondary`, and muted text tokens; browser contrast remains unverified.
- Image quality and asset fidelity: no image assets are part of the requested tab control.
- Copy and content: application tab labels remain unchanged by design.

## Interaction and console checks

- The local server was started, but the browser connection to `localhost:3001` was refused.
- Browser interaction of primary and secondary tabs could not be tested.
- Console inspection could not be completed without a loaded application page.

## Findings

- [P2] Browser-rendered tab comparison is unavailable.
  Location: `web/components/ui/tabs.tsx`.
  Evidence: the source visual is available; the corresponding local page could not load in the in-app browser.
  Impact: the component cannot receive a visual-fidelity sign-off yet.
  Fix: run the local application on a browser-reachable port, capture `/app/organization` with the active tab state, and compare the focused tab control against the reference.

## Implementation Checklist

1. Start the web app on a browser-reachable local port.
2. Capture the Settings tabs with Organization and General selected.
3. Test category and subtab navigation and check console errors.
4. Update this report with the rendered crop and final comparison.

final result: blocked
