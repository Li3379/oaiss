<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-11 -->

# oaiss-chain-frontend

Vue 3 + TypeScript frontend for the OAISS CHAIN carbon accounting platform.

## Key Files

| File | Purpose |
|------|---------|
| `package.json` | Dependencies and scripts |
| `vite.config.js` | Vite build configuration |
| `tsconfig.json` | TypeScript config |
| `env.d.ts` | TypeScript env declarations |
| `index.html` | Entry HTML |
| `Dockerfile` | Docker build |
| `compose.yaml` | Docker Compose |
| `playwright.config.ts` | E2E test config |

## Subdirectories

| Directory | Purpose | Detail |
|-----------|---------|--------|
| `src/` | Application source code | See `src/AGENTS.md` |
| `tests/` | E2E test suites | See `tests/AGENTS.md` |
| `public/` | Static assets | `favicon.svg`, `icons.svg` |
| `docker/` | Express server for production | `server.mjs` |
| `nginx/` | Nginx reverse proxy config | `default.conf` |

## For AI Agents

### Working

- Dev server: `npm run dev` (port 5173)
- Production build: `npm run build`
- Preview production build: `npm run preview`

### Testing

- Unit tests: `npm run test` (Vitest, happy-dom)
- E2E tests: `npx playwright test`

### Patterns

- Vue 3 Composition API with `<script setup>`
- Element Plus for UI components
- vue-i18n for internationalization (zh-CN, en-US)
- Pinia for state management
- ECharts for charts
- TypeScript strict mode

## Dependencies

- **Internal**: oaiss-chain-backend (API server on port 8080)
- **External**: Vue 3.5, TypeScript, Vite 8, Element Plus 2.13, Pinia 3, vue-i18n 11, ECharts 6, Axios
