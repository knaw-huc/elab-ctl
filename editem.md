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