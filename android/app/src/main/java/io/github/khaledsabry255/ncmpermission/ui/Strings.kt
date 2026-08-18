package io.github.khaledsabry255.ncmpermission.ui

import io.github.khaledsabry255.ncmpermission.data.Employee

/**
 * Only the interface is translated. Stored values — names, job titles,
 * departments, addresses, ban reasons — are shown exactly as they sit in the
 * database and are never translated on the fly.
 */
data class Strings(
    val rtl: Boolean,
    val otherLang: String,
    val pinLabel: String,
    val enter: String,
    val checking: String,
    val wrongPin: String,
    val tabSearch: String,
    val tabBanned: String,
    val tabExpiring: String,
    val tabExpired: String,
    val searchHint: String,
    val refresh: String,
    val statActive: String,
    val statResigned: String,
    val statExpiring: String,
    val statBanned: String,
    val code: String,
    val noName: String,
    val resigned: String,
    val present: String,
    val permitStatus: String,
    val grpPersonal: String,
    val grpWork: String,
    val grpSecurity: String,
    val nationalId: String,
    val phone: String,
    val address: String,
    val jobTitle: String,
    val department: String,
    val hireDate: String,
    val banReason: String,
    val resignEnd: String,
    val notSpecified: String,
    val noPhoto: String,
    val download: String,
    val downloading: String,
    val downloaded: String,
    val downloadFail: String,
    val searchResults: String,
    val bannedList: String,
    val expiringList: String,
    val expiredList: String,
    val noMatch: String,
    val noMatchHint: String,
    val noBanned: String,
    val noExpiring: String,
    val noExpired: String,
    val loadFail: String,
    val loadFailHint: String,
    val retry: String
) {
    fun statusLabel(code: String, raw: String?): String = when (code) {
        "BANNED" -> if (rtl) "ممنوع" else "Banned"
        "VALID" -> if (rtl) "ساري" else "Valid"
        "EXPIRING" -> if (rtl) "قرب الانتهاء" else "Expiring Soon"
        "EXPIRED" -> if (rtl) "تصريح منتهي" else "Expired"
        "NO_PERMIT" -> if (rtl) "بدون تصريح" else "No Permit"
        "NO_DATE" -> if (rtl) "بدون تاريخ" else "No Date"
        else -> raw ?: ""
    }

    /** "متبقي 5 أيام" / "5 days left" — Arabic needs the dual and plural forms. */
    fun countdown(days: Int?): String? {
        if (days == null) return null
        if (!rtl) return when {
            days == 0 -> "Expires today"
            days < 0 -> "Expired ${dayWord(-days)} ago"
            else -> "${dayWord(days)} left"
        }
        return when {
            days == 0 -> "ينتهي اليوم"
            days < 0 -> "منتهي منذ ${dayWord(-days)}"
            else -> "متبقي ${dayWord(days)}"
        }
    }

    private fun dayWord(n: Int): String {
        if (!rtl) return if (n == 1) "1 day" else "$n days"
        return when {
            n == 1 -> "يوم واحد"
            n == 2 -> "يومين"
            n in 3..10 -> "$n أيام"
            else -> "$n يوم"
        }
    }

    fun categoryLabel(emp: Employee): String? = when (emp.category) {
        "blue" -> "BLUE COLLAR"
        "white" -> "WHITE COLLAR"
        else -> null
    }

    /** Stored values keep their wording; only this placeholder is translated. */
    fun resignEndText(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return if (raw == "غير محدد") notSpecified else raw
    }

    companion object {
        val AR = Strings(
            rtl = true, otherLang = "EN",
            pinLabel = "الرقم السري", enter = "دخول", checking = "جارٍ التحقق",
            wrongPin = "رمز غير صحيح",
            tabSearch = "بحث", tabBanned = "الممنوعين",
            tabExpiring = "قرب الانتهاء", tabExpired = "منتهية",
            searchHint = "ابحث بالكود أو الرقم القومي أو الاسم", refresh = "تحديث",
            statActive = "الموظفون الموجودون", statResigned = "الاستقالات",
            statExpiring = "قرب الانتهاء", statBanned = "الممنوعون",
            code = "الكود", noName = "اسم غير مسجل",
            resigned = "استقالة", present = "موجود",
            permitStatus = "حالة التصريح",
            grpPersonal = "البيانات الشخصية", grpWork = "بيانات التوظيف",
            grpSecurity = "بيانات أمنية",
            nationalId = "الرقم القومي", phone = "رقم الهاتف", address = "العنوان",
            jobTitle = "الوظيفة", department = "القسم", hireDate = "تاريخ التعيين",
            banReason = "سبب المنع", resignEnd = "انتهاء الاستقالة",
            notSpecified = "غير محدد",
            noPhoto = "لا يوجد صورة", download = "تحميل", downloading = "جارٍ التحميل",
            downloaded = "تم الحفظ", downloadFail = "تعذر التحميل",
            searchResults = "نتائج البحث", bannedList = "قائمة الممنوعين",
            expiringList = "تصاريح قرب الانتهاء", expiredList = "تصاريح منتهية",
            noMatch = "لم يتم العثور على موظف مطابق",
            noMatchHint = "تأكد من الكود أو الرقم القومي أو الاسم",
            noBanned = "لا يوجد موظفون ممنوعون حاليًا",
            noExpiring = "لا توجد تصاريح تنتهي خلال 30 يوم",
            noExpired = "لا توجد تصاريح منتهية",
            loadFail = "تعذر تحميل البيانات",
            loadFailHint = "تحقق من الاتصال بالإنترنت وحاول مرة أخرى",
            retry = "إعادة المحاولة"
        )

        val EN = Strings(
            rtl = false, otherLang = "AR",
            pinLabel = "PIN Code", enter = "Enter", checking = "Checking",
            wrongPin = "Incorrect PIN",
            tabSearch = "Search", tabBanned = "Banned",
            tabExpiring = "Expiring", tabExpired = "Expired",
            searchHint = "Search by code, national ID or name", refresh = "Refresh",
            statActive = "Active employees", statResigned = "Resignations",
            statExpiring = "Expiring soon", statBanned = "Banned",
            code = "CODE", noName = "Name not recorded",
            resigned = "RESIGNED", present = "ACTIVE",
            permitStatus = "Permit Status",
            grpPersonal = "PERSONAL DETAILS", grpWork = "EMPLOYMENT DETAILS",
            grpSecurity = "SECURITY DETAILS",
            nationalId = "National ID", phone = "Phone", address = "Address",
            jobTitle = "Job Title", department = "Department", hireDate = "Hire Date",
            banReason = "Ban Reason", resignEnd = "Resignation Ends",
            notSpecified = "Not specified",
            noPhoto = "No photo available", download = "Download",
            downloading = "Downloading", downloaded = "Saved",
            downloadFail = "Download failed",
            searchResults = "Search Results", bannedList = "Banned List",
            expiringList = "Permits Expiring Soon", expiredList = "Expired Permits",
            noMatch = "No matching employee found",
            noMatchHint = "Check the code, national ID or name",
            noBanned = "No banned employees at the moment",
            noExpiring = "No permits expiring within 30 days",
            noExpired = "No expired permits",
            loadFail = "Could not load data",
            loadFailHint = "Check your internet connection and try again",
            retry = "Try again"
        )

        fun of(lang: String) = if (lang == "en") EN else AR
    }
}
