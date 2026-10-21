FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm test && npm run build

FROM nginx:1.28-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY deploy/render/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY deploy/render/start-frontend.sh /usr/local/bin/start-foundly
RUN sed -i 's/\r$//' /usr/local/bin/start-foundly && chmod 755 /usr/local/bin/start-foundly
EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/start-foundly"]
CMD ["nginx", "-g", "daemon off;"]
