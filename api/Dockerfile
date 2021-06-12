# syntax=docker/dockerfile:1

FROM node:16.3.0
WORKDIR /build
COPY . .
RUN npm install
RUN npm run build

FROM node:16.3.0
ENV NODE_ENV=production
WORKDIR /app
COPY ["package.json", "package-lock.json*", "./"]
RUN npm install --production
COPY --from=0 /build .
CMD ["node", "bin/index.js"]