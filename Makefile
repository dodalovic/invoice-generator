LANG ?= EN

build-jar:
	./gradlew clean shadowJar

make-pdf:
	java -jar build/libs/invoice-generator-1.0-SNAPSHOT-all.jar -l $(LANG)
