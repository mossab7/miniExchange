COMPOSE = docker compose --env-file ./srcs/.env -f ./srcs/docker-compose.yml

all :
	$(COMPOSE) up -d --build

down:
	@$(COMPOSE) down

re:
	@$(COMPOSE) up -d --build

clean:
	@$(COMPOSE) down --rmi all --volumes --remove-orphans


.PHONY: all down re clean