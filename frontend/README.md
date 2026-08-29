# Customer Activity Analytics — frontend

React 18-style SPA (React 19 + TypeScript strict + Vite) for the customer care
console: customer search, activity dashboard (overview cards, SVG activity
chart, transactions, triggered risk rules) and AI risk analyses.

Runtime dependencies are limited to `react`, `react-dom` and
`react-router-dom`; everything else (chart, styling, fetch client) is
hand-rolled. The API contract in `../docs/api-contract.md` is mirrored in
`src/api/types.ts`.

## Commands

- `npm install`
- `npm run dev` — dev server; proxies `/api` to `$VITE_PROXY_TARGET`
  (default `http://localhost:8080`)
- `npm run build` — type-check (`tsc -b`) + production build
- `npm run lint` — oxlint

## Demo credentials

`alice / operator123` (operator), `bob / supervisor123` (supervisor).

## Structure

- `src/api/` — contract types + fetch client (Bearer token, RFC 7807 errors,
  401 handling)
- `src/auth/` — auth context (login, logout, session restore via `/api/auth/me`)
- `src/pages/` — `/login`, `/` (search), `/customers/:id` (dashboard), 404
- `src/features/` — dashboard building blocks (chart, overview cards,
  transactions table, triggered rules, AI analysis panel)
- `src/components/` — shared badges/chips and UI primitives
- `src/styles/` — design tokens, base styles, shared component styles
