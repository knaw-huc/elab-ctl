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

RED=\033[1;31m
GREEN=\033[1;32m
YELLOW=\033[1;33m
BLUE=\033[1;34m
RESET=\033[0m

.PHONY: shadow-jar
shadow-jar:
	@make $(SHADOW_JAR)

$(SHADOW_JAR): build.gradle.kts settings.gradle.kts $(NEWER_SOURCE_FILES)
	./gradlew shadowJar
	@echo
	@touch $@

data/no-image-available.jp2:
	 magick -background lightyellow -fill black -gravity center -size 1000x1700 -pointsize 48 label:"No Image Available" $@

.PHONY: test
test:
	./gradlew test

.PHONY: clean
clean:
	./gradlew clean

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
	#xmllint --valid --noout --relaxng ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-letter.rng build/zip/$(BRICOR)/*/*.xml

out/%/copy-facsimiles.sh: %

data/%-facsimiles.zip: scripts/ec-get-facsimiles-zip.sh | out/%/copy-facsimiles.sh
	./scripts/ec-get-facsimiles-zip.sh $*

# brieven-correspondenten-1900
out/brieven-correspondenten-1900:
	mkdir -p $@

out/brieven-correspondenten-1900/sizes_pages.tsv: data/elab4-brieven-correspondenten-1900.war $(SHADOW_JAR) | data/brieven-correspondenten-1900-facsimiles.zip out/brieven-correspondenten-1900
	./bin/elabctl generate-manifests data/brieven-correspondenten-1900-facsimiles.zip data/elab4-brieven-correspondenten-1900.war

.PHONY: brieven-correspondenten-1900
brieven-correspondenten-1900: | out/brieven-correspondenten-1900
	./bin/elabctl archive ./data/elab4-$(BRICOR).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-about.rng build/zip/$(BRICOR)/about/*.xml > out/brieven-correspondenten-1900/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-letter.rng build/zip/$(BRICOR)/letters/*.xml >> out/brieven-correspondenten-1900/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BRICOR)/schema/editem-bio.list.rng build/zip/$(BRICOR)/apparatus/bio.xml >> out/brieven-correspondenten-1900/xml-validate.log
	less out/brieven-correspondenten-1900/xml-validate.log

.PHONY: brieven-correspondenten-1900-manifests
brieven-correspondenten-1900-manifests: out/brieven-correspondenten-1900/sizes_pages.tsv

.PHONY: brieven-correspondenten-1900-rsync
brieven-correspondenten-1900-rsync:
	rsync -rav out/$(BRICOR)/manifests ~/workspaces/editem/elaborate-export/$(BRICOR)/
	rsync -rav out/$(BRICOR)/metadata ~/workspaces/editem/elaborate-export/$(BRICOR)/
	rsync -rav build/zip/$(BRICOR)/* ~/workspaces/editem/elaborate-export/$(BRICOR)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BRICOR) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-brieven-correspondenten-1900
browse-brieven-correspondenten-1900:
	@open https://gitlab.huc.knaw.nl/eDITem/brieven-correspondenten-1900
	@open https://gitlab.huc.knaw.nl/eDITem/brieven-correspondenten-1900-settings

# correspondentie-bolland-en-cosijn
out/correspondentie-bolland-en-cosijn:
	mkdir -p $@

out/correspondentie-bolland-en-cosijn/sizes_pages.tsv: data/correspondentie-bolland-en-cosijn-facsimiles.zip data/elab4-correspondentie-bolland-en-cosijn.war $(SHADOW_JAR) | out/correspondentie-bolland-en-cosijn
	./bin/elabctl generate-manifests data/correspondentie-bolland-en-cosijn-facsimiles.zip data/elab4-correspondentie-bolland-en-cosijn.war

.PHONY: correspondentie-bolland-en-cosijn
correspondentie-bolland-en-cosijn: | out/correspondentie-bolland-en-cosijn
	./bin/elabctl archive ./data/elab4-$(BOLCOS).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BOLCOS)/schema/editem-about.rng build/zip/$(BOLCOS)/about/*.xml > out/correspondentie-bolland-en-cosijn/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(BOLCOS)/schema/editem-letter.rng build/zip/$(BOLCOS)/letters/*.xml >> out/correspondentie-bolland-en-cosijn/xml-validate.log
	less out/correspondentie-bolland-en-cosijn/xml-validate.log

.PHONY: correspondentie-bolland-en-cosijn-manifests
correspondentie-bolland-en-cosijn-manifests: out/correspondentie-bolland-en-cosijn/sizes_pages.tsv

.PHONY: correspondentie-bolland-en-cosijn-rsync
correspondentie-bolland-en-cosijn-rsync:
	rsync -rav build/zip/$(BOLCOS)/* ~/workspaces/editem/elaborate-export/$(BOLCOS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(BOLCOS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-correspondentie-bolland-en-cosijn
browse-correspondentie-bolland-en-cosijn:
	@open https://gitlab.huc.knaw.nl/eDITem/correspondentie-bolland-en-cosijn
	@open https://gitlab.huc.knaw.nl/eDITem/correspondentie-bolland-en-cosijn-settings

# clusiuscorrespondence
out/clusiuscorrespondence:
	mkdir -p $@

out/clusiuscorrespondence/copy-facsimiles.sh:  | out/clusiuscorrespondence
	./bin/elabctl archive ./data/elab4-$(CLUSIUS).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(CLUSIUS)/schema/editem-about.rng build/zip/$(CLUSIUS)/about/*.xml > out/clusiuscorrespondence/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(CLUSIUS)/schema/editem-letter.rng build/zip/$(CLUSIUS)/letters/*.xml >> out/clusiuscorrespondence/xml-validate.log
	less out/clusiuscorrespondence/xml-validate.log

#data/clusiuscorrespondence-facsimiles.zip: out/clusiuscorrespondence/copy-facsimiles.sh

out/clusiuscorrespondence/sizes_pages.tsv: data/clusiuscorrespondence-facsimiles.zip data/elab4-clusiuscorrespondence.war $(SHADOW_JAR) | out/clusiuscorrespondence
	./bin/elabctl generate-manifests data/clusiuscorrespondence-facsimiles.zip data/elab4-clusiuscorrespondence.war

.PHONY: clusiuscorrespondence
clusiuscorrespondence: out/clusiuscorrespondence/copy-facsimiles.sh

.PHONY: clusiuscorrespondence-manifests
clusiuscorrespondence-manifests: out/clusiuscorrespondence/sizes_pages.tsv

.PHONY: clusiuscorrespondence-rsync
clusiuscorrespondence-rsync:
	rsync -rav build/zip/$(CLUSIUS)/* ~/workspaces/editem/elaborate-export/$(CLUSIUS)/tei/
	cd ~/workspaces/editem/elaborate-export/$(CLUSIUS) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-clusiuscorrespondence
browse-clusiuscorrespondence:
	@open https://gitlab.huc.knaw.nl/eDITem/clusius-correspondence
	@open https://gitlab.huc.knaw.nl/eDITem/clusius-correspondence-settings

# ogier
.PHONY: ogier

out/ogier:
	mkdir -p $@

ogier: | out/ogier
	./bin/elabctl archive ./data/elab4-$(OGIER).war
	echo "validating tei export..."
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(OGIER)/schema/editem-about.rng build/zip/$(OGIER)/about/*.xml > out/ogier/xml-validate.log
	./bin/validate-xml.sh ~/workspaces/editem/elaborate-export/$(OGIER)/schema/editem-letter.rng build/zip/$(OGIER)/manifests/*.xml >> out/ogier/xml-validate.log
	less out/ogier/xml-validate.log

.PHONY: ogier-rsync
ogier-rsync:
	rsync -rav build/zip/$(OGIER)/* ~/workspaces/editem/elaborate-export/$(OGIER)/tei/
	cd ~/workspaces/editem/elaborate-export/$(OGIER) && (git commit -a -m "new elaborate export" && git push)

.PHONY: browse-ogier
browse-ogier:
	@open https://gitlab.huc.knaw.nl/eDITem/ogier
	@open https://gitlab.huc.knaw.nl/eDITem/ogier-settings

.PHONY: help
help:
	@echo -e "make-tools for $(GREEN)$(TAG)$(RESET)"
	@echo
	@echo -e "Please use \`$(YELLOW)make <target>$(RESET)', where $(YELLOW)<target>$(RESET) is one of:"
	@echo -e "  $(BLUE)test$(RESET)          - to test the project"
	@echo -e "  $(BLUE)shadow-jar$(RESET)    - to build the shadow jar build/libs/elabctl.jar"
	@echo -e "  $(BLUE)archive$(RESET)       - to run the archiver"
	@echo -e "  $(BLUE)drafts-list$(RESET)   - to list the available drafts"
	@echo -e "  $(BLUE)editions-list$(RESET) - to list the available editions"
	@echo
	@echo -e "  $(BLUE)all-archives$(RESET) - run the tei export for all elaborate projects"
	@echo
	@echo -e "  $(BLUE)$(BRICOR)$(RESET)           - to run the tei export for $(BRICOR)"
	@echo -e "  $(BLUE)$(BRICOR)-manifests$(RESET) - to generate the manifests for $(BRICOR)"
	@echo -e "  $(BLUE)$(BRICOR)-rsync$(RESET)     - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(BRICOR)"
	@echo -e "  $(BLUE)browse-$(BRICOR)$(RESET)    - to open the $(BRICOR) gitlab repo in your browser"
	@echo
	@echo -e "  $(BLUE)$(BOLCOS)$(RESET)        - to run the tei export for $(BOLCOS)"
	@echo -e "  $(BLUE)$(BOLCOS)-rsync$(RESET)  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(BOLCOS)"
	@echo -e "  $(BLUE)browse-$(BOLCOS)$(RESET) - to open the $(BOLCOS) gitlab repo in your browser"
	@echo
	@echo -e "  $(BLUE)$(CLUSIUS)$(RESET)        - to run the tei export for $(CLUSIUS)"
	@echo -e "  $(BLUE)$(CLUSIUS)-rsync$(RESET)  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(CLUSIUS)"
	@echo -e "  $(BLUE)browse-$(CLUSIUS)$(RESET) - to open the $(CLUSIUS) gitlab repo in your browser"
	@echo
	@echo -e "  $(BLUE)$(OGIER)$(RESET)        - to run the tei export for $(OGIER)"
	@echo -e "  $(BLUE)$(OGIER)-rsync$(RESET)  - to update the letter tei for https://gitlab.huc.knaw.nl/eDITem/$(OGIER)"
	@echo -e "  $(BLUE)browse-$(OGIER)$(RESET) - to open the $(OGIER) gitlab repo in your browser"
