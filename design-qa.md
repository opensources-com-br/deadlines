# Design QA — sidebar user menu

- Source visual truth: `web/.qa/reference-user-menu.png`
- Implementation screenshot: `web/.qa/implementation-user-menu.jpg`
- Combined comparison: `web/.qa/user-menu-comparison.png`
- Browser viewport: 1280 × 720 CSS px
- Source pixels: 567 × 226 at 1×
- Implementation pixels: 1280 × 720 at 1×; focused comparison crop is 467 × 226
- State: desktop, dark theme, expanded sidebar, account menu open

## Full-view comparison evidence

The implementation screenshot confirms the complete desktop layout, the utility actions immediately above the user control, and the popup opening to the right of the sidebar without clipping or covering the trigger.

## Focused region comparison evidence

`web/.qa/user-menu-comparison.png` places the source and implementation side by side at the same 226 px height. The comparison covers the sidebar actions, user trigger, popup header, account options, separators, and logout action.

## Required fidelity surfaces

- Fonts and typography: Geist follows the existing product type system and matches the source's compact sans-serif hierarchy, weights, line heights, and truncation behavior.
- Spacing and layout rhythm: utility items, user card, popup rows, separators, radius, and right-side placement reproduce the source hierarchy. The implementation keeps the product's existing 16rem sidebar token rather than widening it to the screenshot crop.
- Colors and visual tokens: dark background, muted secondary text, hover/active fill, borders, and popup elevation use the existing semantic theme tokens and closely match the source.
- Image quality and asset fidelity: product and UI icons use the existing image asset and Lucide icon library. The user avatar correctly falls back to initials because the current user profile model does not expose an avatar URL.
- Copy and content: Account, Plans, Notifications, and Log out match the requested product terminology; Settings, Get Help, and Search remain above the user control.

## Interaction and console checks

- User button opens and closes the popup.
- Account, Plans, and Notifications each switch to the correct content.
- Team, Organization, Access control, and Security each switch within Settings.
- Get Help and Search provide visible feedback.
- Popup runtime error found during the first pass was fixed by placing the account label inside the required menu group.
- Final browser pass produced no new console errors.

## Findings

No actionable P0, P1, or P2 differences remain.

## Comparison history

- P1: opening the popup initially raised a Base UI menu-group runtime error. Fixed by nesting the popup identity label in `DropdownMenuGroup`; the post-fix capture shows the complete menu and all menu items were exercised successfully.

## Follow-up polish

- P3: add profile avatar URL support when that field becomes available in the user domain model.

final result: passed
