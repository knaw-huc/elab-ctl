all: help
TAG = elab-ctl
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash
SHADOW_JAR=build/libs/elabctl.jar
NEWER_SOURCE_FILES=$(shell find src/main -newer $(SHADOW_JAR) -type f)

.PHONY: shadow-jar
shadow-jar:
	@make $(SHADOW_JAR)

$(SHADOW_JAR): build.gradle.kts settings.gradle.kts $(NEWER_SOURCE_FILES)
	./gradlew shadowJar
	@echo
	@touch $@

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
