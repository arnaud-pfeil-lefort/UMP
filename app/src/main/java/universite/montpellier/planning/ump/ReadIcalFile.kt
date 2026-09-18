package universite.montpellier.planning.ump

import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import android.content.Context


class ReadIcalFile {
    companion object {
        fun getDatas(context: Context): List<VEvent> {
            val content = context.resources.openRawResource(R.raw.adecal).bufferedReader()
            val ical: ICalendar = Biweekly.parse(content).first() // Je pense qu'on doit pouvoir parser directement dans mon type "Cours"
            val events = ical.getEvents()
            return events
        }
    }
}