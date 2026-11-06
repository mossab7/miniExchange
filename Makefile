SHELL := /bin/bash

COMPOSE := docker compose -f srcs/docker-compose.yml
FRONTEND_DIR := frontend
NODE_BIN ?= $(if $(wildcard /home/blackstar/.local/node/bin/node),/home/blackstar/.local/node/bin/node,node)
NPM_BIN ?= $(if $(wildcard /home/blackstar/.local/node/bin/npm),/home/blackstar/.local/node/bin/npm,npm)
export PATH := $(if $(wildcard /home/blackstar/.local/node/bin/node),/home/blackstar/.local/node/bin:$(PATH),$(PATH))

.PHONY: all dev up down logs frontend-install frontend frontend-build frontend-test backend engine test build clean

# One command for local development: infrastructure + backend/engine in Docker,
# and the Vite frontend in the foreground.
all: dev

dev:
	@$(MAKE) up
	@echo "Starting MiniExchange frontend at http://localhost:5173"
	@cd $(FRONTEND_DIR) && $(NPM_BIN) install && $(NPM_BIN) run dev -- --host 0.0.0.0

# Start the complete backend infrastructure in the background.
up:
	@docker rm -f miniExchange_postgres miniExchange_kafka miniExchange_backend miniExchange_matching_engine >/dev/null 2>&1 || true
	@$(COMPOSE) up -d --build

down:
	@$(COMPOSE) down

logs:
	@$(COMPOSE) logs -f

frontend-install:
	@cd $(FRONTEND_DIR) && $(NPM_BIN) install

frontend:
	@cd $(FRONTEND_DIR) && $(NPM_BIN) run dev -- --host 0.0.0.0

frontend-build:
	@cd $(FRONTEND_DIR) && $(NPM_BIN) run build

frontend-test:
	@cd $(FRONTEND_DIR) && $(NPM_BIN) test

backend:
	@cd srcs/backend && ./gradlew :app:bootRun

engine:
	@cmake --build build --target run-matching-engine

test: frontend-test
	@cmake --build build --target run-all

build: frontend-build
	@cmake --build build --parallel

clean:
	@$(COMPOSE) down
