# syntax=docker/dockerfile:1

# 빌드 단계. Node 버전은 .nvmrc 와 맞춘다 — package.json 의 engines 는 >=24 다.
FROM --platform=$BUILDPLATFORM node:24.14.1-alpine AS build
WORKDIR /src

# 의존성 레이어를 소스와 분리한다. package-lock.json 이 안 바뀌면 npm ci 를 건너뛴다.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite 환경변수는 런타임이 아니라 **빌드 시점에 번들에 박힌다**(src/shared/config/env.ts).
# 브라우저는 호스트에서 돌고 백엔드는 8080 을 퍼블리시하므로 기본값이 그대로 맞다.
# S3/CloudFront 용 번들을 만들 때만 --build-arg 로 덮는다.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# tsc -b && vite build. 타입 에러가 여기서 빌드를 세우는 것이 의도다.
RUN npm run build

######
FROM nginx:1.29-alpine

# SPA fallback 과 캐시 정책. 기본 설정을 덮어쓴다.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html

EXPOSE 80
