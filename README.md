# validatie corpus

https://www.elaborate.huygens.knaw.nl/projects/ogier

Lijst met aanpassingen in de tei die niet matchen met de templates

- additionele instap-punten voor de peen pipeline: de corpus in een andere vorm aanbieden.

- images halen van de iiif server van de eigenaar (if exists)
bc1900:
  <!-- Bewaarplaats = Archief Mankunagaran, Surakarta -->
  <!-- Bewaarplaats = Bijzondere Collecties UvA, Amsterdam -->
  <!-- Bewaarplaats = Gemeentearchief Amsterdam -->
  <!-- Bewaarplaats = Gemeentearchief Den Haag -->
  <!-- Bewaarplaats = Internationaal Instituut voor Sociale Geschiedenis, Amsterdam -->
  <!-- Bewaarplaats = Koninklijke Bibliotheek, Den Haag -->
  <!-- Bewaarplaats = Letterkundig Museum, Den Haag -->
  <!-- Bewaarplaats = Museum Meermanno, Den Haag -->
  <!-- Bewaarplaats = Nederlands Muziek Instituut, Den Haag -->
  <!-- Bewaarplaats = Onbekend -->
  <!-- Bewaarplaats = Particulier bezit -->
  <!-- Bewaarplaats = Particuliere collectie Paul Gorter -->
  <!-- Bewaarplaats = Rijksbureau voor Kunsthistorische Documentatie, Den Haag -->
  <!-- Bewaarplaats = Rijksprentenkabinet, Amsterdam -->
  <!-- Bewaarplaats = Stichting Vrienden van de Schilder Martin Monnickendam -->
  <!-- Bewaarplaats = Witsenhuis, Amsterdam -->

# brieven-correspondenten-1900

31 november?
- entry-2775-1887_11_31.xml
- entry-2776-1887_11_31.xml

# clusiuscorrespondence

- heeft 4 textlayers: Transcription (= original), Translation (=translation), Comments, Summary
- Comments en Summary worden in https://clusiuscorrespondence.huygens.knaw.nl/edition niet getoond, maar zitten wel in de data. Bijvoorbeeld in https://www.elaborate.huygens.knaw.nl/projects/clusiuscorrespondence2/entries/185/transcriptions/summary . Opnemen, en zoja, hoe?
- Er zijn ook brieven waarvan alleen de metadata bekend is, bijv. https://clusiuscorrespondence.huygens.knaw.nl/edition/entry/4685 . Dit levert dan een een lege <body> op, en dat mag niet. Hoe gaan we hier mee om?

Na conversie: downloaden facsimiles zip:
scp out/copy-facsimiles.sh tiler: && ssh tiler ./copy-facsimiles.sh && scp tiler:/data/tmp/facsimiles.zip . && ssh tiler rm /data/tmp/facsimiles.zip
