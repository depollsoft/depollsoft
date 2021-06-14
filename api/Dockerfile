# syntax=docker/dockerfile:1

FROM node:16.3.0
WORKDIR /build
COPY . .
RUN npm install
RUN npm run build

FROM node:16.3.0
ARG MAXMIND_LICENSE_KEY
ENV NODE_ENV=production
WORKDIR /app
COPY ["package.json", "package-lock.json*", "./"]
RUN npm install --production
RUN cd node_modules/geoip-lite && npm run-script updatedb license_key=$MAXMIND_LICENSE_KEY
COPY --from=0 /build .

CMD ["node", "bin/index.js"]