all: help
TAG = elab-ctl
DOCKER_DOMAIN = registry.diginfra.net/tt
SHELL=/bin/bash
SHADOW_JAR=build/libs/elabctl.jar
NEWER_SOURCE_FILES=$(shell find src/main -newer $(SHADOW_JAR) -type f)
BRICOR=brieven-correspondenten-1900
BOLCOS=correspondentie-bolland-en-cosijn
CLUSIUS=clusiuscorrespondence
OGIER=ogier

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

.PHONY: all-archives
all-archives:
	./bin/elabctl archive ./data/elab4-$(BRICOR).war ./data/elab4-$(BOLCOS).war ./data/elab4-$(CLUSIUS).war
	#xmllint --valid --noout --relaxng ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-letter.rng build/zip/elab4-$(BRICOR)/*/*.xml

# brieven-correspondenten-1900
.PHONY: brieven-correspondenten-1900
brieven-correspondenten-1900:
	./bin/elabctl archive ./data/elab4-$(BRICOR).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-about.rng build/zip/elab4-$(BRICOR)/about/*.xml > out/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-letter.rng build/zip/elab4-$(BRICOR)/letters/*.xml >> out/xml-validate.log
	less out/xml-validate.log

.PHONY: brieven-correspondenten-1900-rsync
brieven-correspondenten-1900-rsync:
	rsync -rav build/zip/elab4-$(BRICOR)/* ~/workspaces/editem/elaborate-export/$(BRICOR)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BRICOR) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-brieven-correspondenten-1900
browse-brieven-correspondenten-1900:
	@open https://gitlab.huc.knaw.nl/eDITem/brieven-correspondenten-1900
	@open https://gitlab.huc.knaw.nl/eDITem/brieven-correspondenten-1900-settings

# correspondentie-bolland-en-cosijn
.PHONY: correspondentie-bolland-en-cosijn
correspondentie-bolland-en-cosijn:
	./bin/elabctl archive ./data/elab4-$(BOLCOS).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BOLCOS)/schema/editem-about.rng build/zip/elab4-$(BOLCOS)/about/*.xml > out/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BOLCOS)/schema/editem-letter.rng build/zip/elab4-$(BOLCOS)/letters/*.xml >> out/xml-validate.log
	less out/xml-validate.log

.PHONY: correspondentie-bolland-en-cosijn-rsync
correspondentie-bolland-en-cosijn-rsync:
	rsync -rav build/zip/elab4-$(BOLCOS)/* ~/workspaces/editem/elaborate-export/$(BOLCOS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BOLCOS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-correspondentie-bolland-en-cosijn
browse-correspondentie-bolland-en-cosijn:
	@open https://gitlab.huc.knaw.nl/eDITem/correspondentie-bolland-en-cosijn
	@open https://gitlab.huc.knaw.nl/eDITem/correspondentie-bolland-en-cosijn-settings

# clusiuscorrespondence
.PHONY: clusiuscorrespondence
clusiuscorrespondence:
	./bin/elabctl archive ./data/elab4-$(CLUSIUS).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(CLUSIUS)/schema/editem-about.rng build/zip/elab4-$(CLUSIUS)/about/*.xml > out/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(CLUSIUS)/schema/editem-letter.rng build/zip/elab4-$(CLUSIUS)/letters/*.xml >> out/xml-validate.log
	less out/xml-validate.log

.PHONY: clusiuscorrespondence-rsync
clusiuscorrespondence-rsync:
	rsync -rav build/zip/elab4-$(CLUSIUS)/* ~/workspaces/editem/elaborate-export/$(CLUSIUS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(CLUSIUS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-clusiuscorrespondence
browse-clusiuscorrespondence:
	@open https://gitlab.huc.knaw.nl/eDITem/clusius-correspondence
	@open https://gitlab.huc.knaw.nl/eDITem/clusius-correspondence-settings

# ogier
.PHONY: ogier
ogier:
	./bin/elabctl archive ./data/elab4-$(OGIER).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(OGIER)/schema/editem-about.rng build/zip/elab4-$(OGIER)/about/*.xml > out/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(OGIER)/schema/editem-letter.rng build/zip/elab4-$(OGIER)/letters/*.xml >> out/xml-validate.log
	less out/xml-validate.log

.PHONY: ogier-rsync
ogier-rsync:
	rsync -rav build/zip/elab4-$(OGIER)/* ~/workspaces/editem/elaborate-export/$(OGIER)/tei/
	cd ~/workspaces/editem/elaborate-export/$(OGIER) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-ogier
browse-ogier:
	@open https://gitlab.huc.knaw.nl/eDITem/ogier
	@open https://gitlab.huc.knaw.nl/eDITem/ogier-settings

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
	@echo "  all-archives - run the tei export for all elaborate projects"
	@echo
	@echo "  $(BRICOR)        - to run the tei export for $(BRICOR)"
	@echo "  $(BRICOR)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(BRICOR)"
	@echo "  browse-$(BRICOR) - to open the $(BRICOR) gitlab repo in your browser"
	@echo
	@echo "  $(BOLCOS)        - to run the tei export for $(BOLCOS)"
	@echo "  $(BOLCOS)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(BOLCOS)"
	@echo "  browse-$(BOLCOS) - to open the $(BOLCOS) gitlab repo in your browser"
	@echo
	@echo "  $(CLUSIUS)        - to run the tei export for $(CLUSIUS)"
	@echo "  $(CLUSIUS)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(CLUSIUS)"
	@echo "  browse-$(CLUSIUS) - to open the $(CLUSIUS) gitlab repo in your browser"
	@echo
	@echo "  $(OGIER)        - to run the tei export for $(OGIER)"
	@echo "  $(OGIER)-rsync  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(OGIER)"
	@echo "  browse-$(OGIER) - to open the $(OGIER) gitlab repo in your browser"
