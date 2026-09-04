COMPOSE = docker compose --env-file ./srcs/.env -f ./srcs/docker-compose.yml

all :
	$(COMPOSE) up -d --build

down:
	@$(COMPOSE) down

re:
	@$(COMPOSE) up -d --build

test:
	RUN_TESTS=true $(COMPOSE) build --build-arg RUN_TESTS=true backend
	RUN_TESTS=true $(COMPOSE) up -d

clean:
	@$(COMPOSE) down --rmi all --volumes --remove-orphans


.PHONY: all down re clean test