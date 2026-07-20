# app-iteizers Week 11 Update

July 13 - 20

- [Edmond] Added accessible empty-states to the map: instead of rendering blank, the campus map now shows a "No study spots to show" card (icon + text, screen-reader friendly, not color-dependent) and the building-spaces screen shows a message when a building has no listed spaces. Also extracted the map's hardcoded camera values into a single `MapConfig` behind pure display-state functions, added 11 unit tests, and fixed a low-contrast (WCAG AA) issue on the new empty-state text.
