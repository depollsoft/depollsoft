# syntax=docker/dockerfile:1

FROM node:24-bookworm-slim
WORKDIR /build
COPY . .
RUN npm ci
RUN npm run build

FROM node:24-bookworm-slim
ENV NODE_ENV=production
WORKDIR /app
COPY ["package.json", "package-lock.json*", "./"]
RUN npm ci --omit=dev
COPY ./node_modules/geoip-lite/data ./node_modules/geoip-lite/data
COPY --from=0 /build/bin ./bin
RUN ls

CMD ["node", "bin/index.js"]