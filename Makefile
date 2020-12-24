ARGS ?:

build-jar:
	./gradlew clean shadowJar

generate: ./exec
	./exec $(ARGS)