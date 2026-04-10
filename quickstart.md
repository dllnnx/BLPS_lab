# Запуск ozon-service без контейнеров

1) Загрузить wildfly
2) ./bin/add-user.sh и создать Managment user
3) curl -L -o $WILDFLYDIR/postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
4) $WILDFLYDIR/bin/jboss-cli.sh --file=./ozon-ear/configure-datasource.cli
5) mvn install 
6) mvn package
7) Загрузить ozon-ear.ear в вайлдфлай