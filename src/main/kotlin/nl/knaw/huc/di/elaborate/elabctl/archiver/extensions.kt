package nl.knaw.huc.di.elaborate.elabctl.archiver

fun String.asIsoLang() =
    when {
        contains("Nederlands") || contains("Dutch") -> "nl"
        contains("Duits") || contains("German") -> "ge"
        contains("Engels") || contains("English") -> "en"
        contains("Frans") || contains("French") -> "fr"
        contains("Italiaans") || contains("Italian") -> "it"
        contains("Spaans") || contains("Spanish") -> "es"
        contains("Latijns") || contains("Latin") -> "la"
        contains("Unknown") || contains("Onbekend") -> "XX"
        else -> "nl"
    }

fun <T> List<T>.splitOn(predicate: (T) -> Boolean): List<List<T>> {
    val containers = mutableListOf<MutableList<T>>()
    var container = mutableListOf<T>()
    forEach {
        if (predicate(it)) {
            if (container.isNotEmpty()) {
                containers.add(container)
                container = mutableListOf()
            }
        }
        container.add(it)
    }
    containers.add(container)
    return containers
}

