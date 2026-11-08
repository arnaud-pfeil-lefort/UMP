package universite.montpellier.planning.ump

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import universite.montpellier.planning.ump.data.IcalParser

class IcalParserTest {

    @Test
    fun descriptionAde_extraitMatiereEnseignantEtGroupes() {
        val d = IcalParser.analyserDescription(
            "\n\nL2 Informatique - Gr B\nProgrammation web\nMEGHAZI   IMRAN\nA valider\n(Exported :26/09/2026 16:05)\n"
        )
        assertEquals("Programmation web", d.matiere)
        assertEquals(listOf("Meghazi Imran"), d.enseignants)
        assertEquals(listOf("L2 Informatique - Gr B"), d.groupes)
    }

    @Test
    fun descriptionVide() {
        val d = IcalParser.analyserDescription("")
        assertNull(d.matiere)
        assertEquals(emptyList<String>(), d.enseignants)
    }

    @Test
    fun fichierEmbarque_parseEtCouleursDistinctesParMatiere() {
        val contenu = javaClass.getResource("/adecal.ics")!!.readText()
        val cours = IcalParser.parse(contenu)
        assert(cours.isNotEmpty())

        val web = cours.first { it.code == "HAI305I" }
        assertEquals("Programmation web", web.nom)

        val couleurParCode = cours.filter { it.code != null }.groupBy { it.code }.mapValues { (_, l) -> l.map { it.couleur }.distinct() }
        couleurParCode.values.forEach { assertEquals(1, it.size) }
        assertNotEquals(couleurParCode["HAI305I"], couleurParCode["HAI306X"])
    }
}
