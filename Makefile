db:
	docker-compose up -d db

stop-db:
	docker-compose stop db

test: db
	export POSTGRES_USER=postgres && export POSTGRES_PASSWORD=postgres && export POSTGRES_URL=localhost:5432/postgres && mvn test;

publish:
	mvn -DskipTests clean deploy
