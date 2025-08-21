De tei header in de editem templates is redelijk variabel, en er wordt gebruik gemaakt van gegevens die niet standaard in de elaborate werkomgeving bij het project ingevuld zijn.

Ik neig ernaar om per project een tei template aan te (laten) maken, met daarin via `{variabele}` constructies de mogelijkheid houden om er per entry data in te laten vullen uit de layers, de annotaties en de metadatavelden.

- per project alvast zo'n template genereren, en daarin aangeven welke variabelen er gebruikt kunnen worden.


# facsimile

de scans zijn wel te achterhalen uit elaborate, maar de pagina indeling is lastiger: er is geen standaard manier om paginagrenzen aan te geven in de lopende tekst. Niet alle projecten zullen dan ook duidelijke pagina grenzen hebben.
brieven-correspondenten gebruikt ¶ om pagebreaks aan te geven.
als er een standaard verzonnen kan worden om pagebreaks in de lopende tekst op te nemen zouden de project editors dat nog moeten doen.
als we bij de graphic url vast willen houden aan een iiif url moeten de images gehost worden op een iiif server

# text sections

In elaborate kunnen verschillende text layers gedefinieerd zijn. De namen daarvan zullen niet altijd overeenkomen met de types zoals in https://editem.pages.huc.knaw.nl/editem-schema/templates/general/general-encoding.html#text-sections gedefinieerd.
Daar zal een mapping voor moeten komen. Bij deze mapping kan dan ook de taal opgenomen worden.

# pages

zie facsimile

# headings

headings zijn ook niet op een standaard manier gecodeerd in de lopende tekst.

# paragraphs

Bij de lopende tekst wordt nu de newline aangehouden als paragraph grens. Het kan ook een optie zijn om een lege tussenregel als paragraafgrens te houden. Dit zal dan per project ingesteld moeten worden.
xml:id en n kunnen gegenereerd worden vanuit de (gemapte) div type

## corresp bij translations

als er een translation layer is, en het aantal afgeleidde paragrafen is hetzelfde als bij de original layer, dan kan het corresp attribuut afgeleid worden, ervanuitgaande dat de paragraph indeling bij original en transaltion hetzelfde is.

# transcriptional elements

de stylings die de elaborate editor mogelijk maakt zijn vertaald naar de html codes, en deze worden  gemapt naar hi rend elementen. Ondersteunde rend values: bold underline italics super
subscript kan ook nog voorkomen, maar daar zie ik geen rend value voor in https://editem.pages.huc.knaw.nl/editem-schema/templates/general/general-encoding.html#styling-of-the-original-document
in brieven-correspondenten zijn sommige onderstrepingen in de oorspronkelijke tekst vertaald naar italics in de transcriptie

# vertical spaces

vertical spaces kunnen gecodeerd zijn mbv meervoudige lege regels. Deze cases kunnen omgezet worden in <space>
In de project config kan aangegeven worden of de lege regels zo geïnterpreteerd moeten worden.

# interventions 

Dit soort interventions zal ook niet op een standaard manier in de lopende tekst zitten.

In  brieven-correspondenten komen in de lopende tekst wel constructies voor als:

`nie[u?]w` maar bij de interpretatie hiervan zal de verantwoordelijke geraadpleegd moeten worden. 

# internal references

## text to notes

Er komen annotaties voor in de tekst, deze kunnen vertaald worden naar een standoff annotation sectie + pointers in de tekst
annotaties hebben ook nog een type, waar kan ik dat kwijt in de <note> ?

---

# correspondentie-bolland-en-cosijn

Het correspondentie-bolland-en-cosijn heb ik ook geconverteerd, op enkele punten heb ik de code moeten aanpassen.

De gegenereerde TEI is hier te zien:
https://gitlab.huc.knaw.nl/elaborate/correspondentie-bolland-en-cosijn/-/tree/main/tei?ref_type=heads

Voor de `<editor>` heb ik een aanname gedaan, als dat anders moet zijn hoor ik het wel.


# `about` teksten

Via een xml dump vanuit WordPress is de html tekst van de about pages te achterhalen.

De semi-geconverteerde versies staan hier:

- voor correspondentie-bolland-en-cosijn: https://gitlab.huc.knaw.nl/elaborate/correspondentie-bolland-en-cosijn/-/tree/main/tei/about
- voor brieven-correspondenten-1900: https://gitlab.huc.knaw.nl/elaborate/brieven-correspondenten-1900/-/tree/main/tei/about

Zijn ze allemaal relevant?
Voor brieven-correspondenten zitten er zo te zien (eerdere versies van?) blogposts van https://opgravingen.huygens.knaw.nl/
bij. Zijn die relevant?

In sommige gevallen was de HTML niet well-formed, omdat begin/end tags ontbraken.
In deze gevallen heb ik de HTML aangepast in de WordPress export file.

De gebruikte HTML tags moeten geconverteerd worden naar TEI/XML tags. Voor sommige ligt de conversie voor de hand, voor andere minder.

Hier is een overzicht van de HTML tags + hun attributen zoals gebruikt in de HTML van de WordPress pagina's.
Waar ik al een conversie zag heb ik deze ingevuld.
Kunnen jullie bij de overige tags en attributen
aangeven hoe deze naar Editem TEI geconverteerd moeten worden?

| HTML | attr | TEI | attr             | comment |
|------|------|-----|------------------|---------|
| a | href | ref | target           |         |
|  | name |     |                  |         |
| button | onclick |     |                  |         |
|  | style |     |                  |         |
| em |  | hi  | rend="italics"   |         |
| h2 |  |     |                  |         |
| h3 | style |     |                  |         |
| i | style | hi  | rend="italics"   |         |
| iframe | allowfullscreen |     |                  |         |
|  | frameborder |     |                  |         |
|  | height |     |                  |         |
|  | src |     |                  |         |
|  | width |     |                  |         |
| img | alt |     |                  |         |
|  | class |     |                  |         |
|  | height |     |                  |         |
|  | src |     |                  |         |
|  | style |     |                  |         |
|  | width |     |                  |         |
| li |  |     |                  |         |
| ol |  |     |                  |         |
| p | style | p   |                  |         |
| span | style |     |                  |         |
| strong |  | hi  | rend="bold"      |         |
| sup |  | hi  | rend="super"     |         |
| table | width |     |                  |         |
| tbody |  |     |                  |         |
| td | style |     |                  |         |
|  | width |     |                  |         |
| tr |  |     |                  |         |
| u |  | hi  | rend="underline" |         |
| ul |  |     |                  |         |


## Brieftitels

Voor nu neem ik de aangegeven entry titel als brieftitel, maar Elli gaf aan dat er een vast template is voor de brieftitel, met afzender, ontvanger, verzendplaats en verzend datum.
Bij correspondenten-1900 zitten wat entry titels die zich niet zo laten omvormen, bijv:

- Addens, Johannes aan Verwey, Albert/Vloten, Katharina van | 1890-06-XX ( 2 geadresseerden)
-  Addens, Johannes/Wohltat, Wilhelmina Dorothea aan Gennep, Johanna Elizabeth Hendrika Christina van | 1883-11-14 (2 afzenders)
- Alberdingk Thijm, Karel Joan Lodewijk aan Groesbeek, Klaas/Nijhoff, Paulus#Scheltema & Holkema’s Boekhandel, Uitgeverij | 1902-12-16 (geadresseerde vertegenwoordigd een organisatie)
-  Alberdingk Thijm, Karel Joan Lodewijk aan Verwey, Albert | 1891-01-16+1891-02-09 (meerdere/onzekere verzenddatums) 
-  Jelgersma, Gerbrandus aan Kloos, Willem Johannes Theodorus --> Witsen, Willem Arnoldus | 1895-05-10 (doorgestuurde brief)

Hoe moet de standaard brieftitel er in deze gevallen uitzien?