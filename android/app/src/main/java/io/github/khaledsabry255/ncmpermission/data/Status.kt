package io.github.khaledsabry255.ncmpermission.data

enum class Tone { OK, WARN, BAD, MUTE }

/** [code] is language-independent; the label shown to the user comes from Strings. */
data class PermitStatus(val tone: Tone, val code: String, val rawLabel: String? = null)

/**
 * The permit DATE outranks the stored label, so a row still marked "ساري"
 * whose date has passed reads as expired. The Arabic values below are the ones
 * stored in the database and are matched as-is in both languages.
 */
object Status {

    fun of(emp: Employee): PermitStatus {
        val raw = emp.permitStatus?.trim().orEmpty()
        val days = Dates.daysLeft(emp.permitDate)

        if (raw == "ممنوع") return PermitStatus(Tone.BAD, "BANNED")

        if (raw == "ساري") {
            return when {
                days == null -> PermitStatus(Tone.MUTE, "NO_DATE")
                days < 0 -> PermitStatus(Tone.BAD, "EXPIRED")
                days <= Config.EXPIRY_WINDOW -> PermitStatus(Tone.WARN, "EXPIRING")
                else -> PermitStatus(Tone.OK, "VALID")
            }
        }
        if (raw == "منتهي الصلاحية") return PermitStatus(Tone.BAD, "EXPIRED")
        if (raw.isEmpty() || raw == "بدون تصريح") return PermitStatus(Tone.MUTE, "NO_PERMIT")

        // Anything unexpected is surfaced as stored rather than relabelled away.
        return PermitStatus(Tone.MUTE, "UNKNOWN", raw)
    }
}
