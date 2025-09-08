all: help
TAG = elab-ctl
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash
SHADOW_JAR=build/libs/elabctl.jar
NEWER_SOURCE_FILES=$(shell find src/main -newer $(SHADOW_JAR) -type f)
BRICOR=brieven-correspondenten-1900
BOLCOS=correspondentie-bolland-en-cosijn
CLUSIUS=clusiuscorrespondence

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
	./scripts/e4-list.sh drafts

.PHONY: editions-list
editions-list:
	./scripts/e4-list.sh editions

# brieven-correspondenten-1900
.PHONY: brieven-correspondenten-1900
brieven-correspondenten-1900:
	./bin/elabctl archive ./data/elab4-$(BRICOR).war
	#xmllint --valid --noout --relaxng ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-letter.rng build/zip/elab4-$(BRICOR)/*/*.xml

.PHONY: brieven-correspondenten-1900-rsync
brieven-correspondenten-1900-rsync:
	rsync -rav build/zip/elab4-$(BRICOR)/* ~/workspaces/editem/elaborate-export/$(BRICOR)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BRICOR) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-brieven-correspondenten-1900
browse-brieven-correspondenten-1900:
	@open https://gitlab.huc.knaw.nl/elaborate/brieven-correspondenten-1900

# correspondentie-bolland-en-cosijn
.PHONY: correspondentie-bolland-en-cosijn
correspondentie-bolland-en-cosijn:
	./bin/elabctl archive ./data/elab4-$(BOLCOS).war
	#xmllint --valid --noout --relaxng ~/workspaces/editem/elaborate-export/$(BOLCOS)/schema/editem-letter.rng build/zip/elab4-$(BOLCOS)/*/*.xml

.PHONY: correspondentie-bolland-en-cosijn-rsync
correspondentie-bolland-en-cosijn-rsync:
	rsync -rav build/zip/elab4-$(BOLCOS)/* ~/workspaces/editem/elaborate-export/$(BOLCOS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BOLCOS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-correspondentie-bolland-en-cosijn
browse-correspondentie-bolland-en-cosijn:
	@open https://gitlab.huc.knaw.nl/elaborate/correspondentie-bolland-en-cosijn

# clusiuscorrespondence
.PHONY: clusiuscorrespondence
clusiuscorrespondence:
	./bin/elabctl archive ./data/elab4-$(CLUSIUS).war
	#xmllint --valid --noout --relaxng ~/workspaces/editem/elaborate-export/$(CLUSIUS)/schema/editem-letter.rng build/zip/elab4-$(CLUSIUS)/*/*.xml

.PHONY: clusiuscorrespondence-rsync
clusiuscorrespondence-rsync:
	rsync -rav build/zip/elab4-$(CLUSIUS)/* ~/workspaces/editem/elaborate-export/$(CLUSIUS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(CLUSIUS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-clusiuscorrespondence
browse-clusiuscorrespondence:
	@open https://gitlab.huc.knaw.nl/elaborate/clusiuscorrespondence

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
	@echo "  $(BRICOR)        - to run the tei export for $(BRICOR)"
	@echo "  $(BRICOR)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/elaborate/$(BRICOR)"
	@echo "  browse-$(BRICOR) - to open the $(BRICOR) gitlab repo in your browser"
	@echo
	@echo "  $(BOLCOS)        - to run the tei export for $(BOLCOS)"
	@echo "  $(BOLCOS)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/elaborate/$(BOLCOS)"
	@echo "  browse-$(BOLCOS) - to open the $(BOLCOS) gitlab repo in your browser"
	@echo
	@echo "  $(CLUSIUS)        - to run the tei export for $(CLUSIUS)"
	@echo "  $(CLUSIUS)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/elaborate/$(CLUSIUS)"
	@echo "  browse-$(CLUSIUS) - to open the $(CLUSIUS) gitlab repo in your browser"

