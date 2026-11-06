# MiniExchange frontend

React + TypeScript/Vite terminal for the existing Spring Boot API.

## Run

Install Node.js 20+ and npm, then:

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`. Set `VITE_API_BASE_URL` to a different base URL when needed.

## Backend coverage

The UI uses the implemented endpoints `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/assets`, `GET /api/orders`, `POST /api/orders`, `DELETE /api/orders/{id}`, and `GET /api/portfolios/{portfolioName}`. Decimal prices and quantities remain strings through the request boundary.

The current backend does not expose browser-facing order-book, historical market data, positions, trade-history, WebSocket, or SSE endpoints. Those areas are shown as unavailable rather than populated with simulated financial data. Portfolio order placement also requires a portfolio UUID returned by the portfolio endpoint.

## Admin workspace

Users whose JWT contains the backend `ADMIN` role can open `/admin` to manage the asset registry. The dashboard uses `POST /api/assets` to create assets and `PUT /api/assets/{symbol}` to enable or disable them. It also shows API reachability and asset counts. The backend currently has no admin metrics, user analytics, engine telemetry, or audit-log endpoints, so those are not simulated.
