package universite.montpellier.planning.ump.ui.theme

import android.os.Debug
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import java.io.File
import android.content.Context
import universite.montpellier.planning.ump.R


class ReadFile {
    companion object {
        fun getDatas(context: Context): List<VEvent> {
            val content = context.resources.openRawResource(R.raw.adecal).bufferedReader()
            val ical: ICalendar = Biweekly.parse(content).first() // peut etre pouvoir parser directement dans mon type "Cours"
            val events = ical.getEvents()
            return events
        }
    }
}