all: help
TAG = elab-ctl
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash


.PHONY: shadow-jar
shadow-jar:
	make build/libs/elabctl.jar

build/libs/elabctl.jar: $(shell find src/main -type f) build.gradle.kts settings.gradle.kts
	./gradlew shadowJar

.PHONY: tests
tests:
	./gradlew test

.PHONY: archive
archive:
	./gradlew -q run --args "archive"

.PHONY: drafts-list
drafts-list:
	./scripts/e4-list-drafts.sh

.PHONY: editions-list
editions-list:
	./scripts/e4-list-editions.sh

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo
	@echo "Please use \`make <target>', where <target> is one of:"
	@echo "  tests         - to test the project"
	@echo "  shadow-jar    - to build the shadow jar build/libs/elabctl.jar"
	@echo "  archive       - to run the archiver"
	@echo "  drafts-list   - to list the available drafts"
	@echo "  editions-list - to list the available editions"

	@echo
