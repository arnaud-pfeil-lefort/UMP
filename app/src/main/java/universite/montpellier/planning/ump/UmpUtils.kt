package universite.montpellier.planning.ump

import java.time.LocalDateTime

class UmpUtils {
    companion object {

        fun filtreCours(courss: List<Cours>, date: LocalDateTime): List<Cours> {
            val coursFiltres = ArrayList<Cours>()
            for (cours in courss) {
                if (cours.dateDebut.getDayOfYear().equals(date.getDayOfYear())) {
                    coursFiltres.add(
                        cours
                    )
                }
            }
            return coursFiltres
        }
    }
}