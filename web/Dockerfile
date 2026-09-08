FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
ARG API_URL=http://localhost:8080
ENV API_URL=$API_URL
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S opensources && adduser -S -G opensources opensources
COPY --from=build /app/public ./public
COPY --from=build --chown=opensources:opensources /app/.next/standalone ./
COPY --from=build --chown=opensources:opensources /app/.next/static ./.next/static
USER opensources
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
