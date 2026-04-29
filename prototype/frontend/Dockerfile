FROM node:22-alpine AS build
WORKDIR /app

ARG API_BASE_URL=http://localhost:8080
ENV API_BASE_URL=$API_BASE_URL

COPY package.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN mkdir -p public
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
