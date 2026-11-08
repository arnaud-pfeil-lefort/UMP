package universite.montpellier.planning.ump.data

import biweekly.Biweekly
import universite.montpellier.planning.ump.Cours
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

object IcalParser {

    private val ZONE: ZoneId = ZoneId.of("Europe/Paris")
    private val REGEX_CODE = Regex("""^[A-Z]{2,}\d[A-Z0-9]*$""")
    private val REGEX_GROUPE = Regex("""\bGr\b|^(TD|TP|CM)\b|^(L\d|M\d|DU|Double)""")
    private val ESPACES = Regex("""\s+""")
    /** ADE remplace l'apostrophe typographique par « ? » (ex. « d?information »). */
    private val APOSTROPHE_PERDUE = Regex("""(?<=\p{L})\?(?=\p{L})""")

    private fun reparer(texte: String) = texte.replace(APOSTROPHE_PERDUE, "’")

    private class Brut(
        val code: String?, val nom: String, val precision: String,
        val debut: LocalDateTime, val fin: LocalDateTime, val salle: String,
        val enseignants: List<String>, val groupes: List<String>,
    ) {
        val cle get() = code ?: nom
    }

    /** Parse le contenu texte d'un fichier .ics. Lève une exception si le contenu n'est pas un calendrier valide. */
    fun parse(contenu: String): List<Cours> {
        val ical = Biweekly.parse(contenu).first()
            ?: throw IllegalArgumentException("Calendrier iCal invalide")

        val bruts = ical.events.mapNotNull { event ->
            val debut = event.dateStart?.value ?: return@mapNotNull null
            val fin = event.dateEnd?.value ?: debut
            val titre = reparer(event.summary?.value?.trim().orEmpty())
            val desc = analyserDescription(event.description?.value.orEmpty())

            val code = titre.substringBefore(' ').takeIf { REGEX_CODE.matches(it) }
            val precision = if (code != null) titre.substringAfter(' ', "").trim() else ""
            val nom = when {
                code == null -> titre.ifEmpty { "Événement" }
                desc.matiere != null -> desc.matiere
                else -> titre
            }

            Brut(
                code = code,
                nom = nom,
                precision = precision,
                debut = LocalDateTime.ofInstant(debut.toInstant(), ZONE),
                fin = LocalDateTime.ofInstant(fin.toInstant(), ZONE),
                salle = event.location?.value?.trim().orEmpty(),
                enseignants = desc.enseignants,
                groupes = desc.groupes,
            )
        }

        val indexCouleur = bruts.map { it.cle }.distinct().sorted()
            .withIndex().associate { (i, cle) -> cle to i }

        return bruts.map {
            Cours(
                code = it.code,
                nom = it.nom,
                precision = it.precision,
                dateDebut = it.debut,
                dateFin = it.fin,
                salle = it.salle,
                enseignants = it.enseignants,
                groupes = it.groupes,
                couleur = indexCouleur.getValue(it.cle),
            )
        }.sortedBy { it.dateDebut }
    }

    data class Description(val matiere: String?, val enseignants: List<String>, val groupes: List<String>)
    fun analyserDescription(description: String): Description {
        val lignes = description.lines()
            .map { reparer(it.trim().replace(ESPACES, " ")) }
            .filter { it.isNotEmpty() && !it.startsWith("(Exported") && !it.equals("A valider", ignoreCase = true) }

        val groupes = lignes.filter { REGEX_GROUPE.containsMatchIn(it) }
        val autres = lignes.filterNot { it in groupes }
        val enseignants = autres.filter(::estEnMajuscules).map(::enCasseTitre)
        val matiere = autres.firstOrNull { !estEnMajuscules(it) }
        return Description(matiere, enseignants, groupes)
    }

    private fun estEnMajuscules(ligne: String) =
        ligne.any { it.isLetter() } && ligne == ligne.uppercase(Locale.FRENCH)

    private fun enCasseTitre(ligne: String) =
        ligne.lowercase(Locale.FRENCH).split(' ').joinToString(" ") { mot ->
            mot.replaceFirstChar { it.titlecase(Locale.FRENCH) }
        }
}
