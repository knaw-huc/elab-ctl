all: help
TAG = elab-ctl
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash

.PHONY: tests
tests:
	./gradlew test

.PHONY: archive
archive:
	./gradlew -q run --args "archive"

.PHONY: help
help:
	@echo "make-tools for $(TAG)"
	@echo
	@echo "Please use \`make <target>', where <target> is one of:"
	@echo "  tests         - to test the project"
	@echo "  archive       - to run the archiver"
	@echo
