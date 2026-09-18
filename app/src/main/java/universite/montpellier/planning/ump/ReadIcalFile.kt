package universite.montpellier.planning.ump

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


class ReadIcalFile {
    companion object {
        fun getDatas(context: Context): List<Cours> {
            val content = context.resources.openRawResource(R.raw.adecal).bufferedReader()
            val ical: ICalendar = Biweekly.parse(content).first() // Je pense qu'on doit pouvoir parser directement dans mon type "Cours"
            val events = ical.getEvents()
            val courss = ArrayList<Cours>()
            for (event in events) {
                System.out.println(event.dateStart.value.toString())
                val longToDateDebut = Instant.ofEpochMilli(event.dateStart.value.time)
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDateTime()
                val longToDateFin = Instant.ofEpochMilli(event.dateEnd.value.time)
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDateTime()
                val cours =
                    Cours(
                        nom = event.summary.value,
                        dateDebut = longToDateDebut,
                        dateFin = longToDateFin,
                        salle = event.location.value.toString(),
                    )
                courss.add(cours)
            }
            return courss
        }
    }
}