# SBQS Frontend

Angular web client for Smart Banking Queue System. Page components are loaded by route, while reusable UI lives under `src/app/shared` and application-wide state, guards, models, and API clients live under `src/app/core`.

## Commands

```bash
npm ci
npm start
npm run test:ci
npm run build
```

The development server runs at `http://localhost:4200` and expects the backend API at `http://localhost:8081`.
