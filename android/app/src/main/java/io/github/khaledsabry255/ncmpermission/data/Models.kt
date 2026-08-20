package io.github.khaledsabry255.ncmpermission.data

import org.json.JSONObject

data class Employee(
    val id: String,
    val code: String,
    val nationalId: String?,
    val nameAr: String?,
    val nameEn: String?,
    val jobTitle: String?,
    val department: String?,
    val category: String?,
    val phone: String?,
    val hireDate: String?,
    val address: String?,
    val permitDate: String?,
    val permitStatus: String?,
    val banReason: String?,
    val resignationEnd: String?,
    val notes: String?
) {
    /**
     * Any value counts, not just a readable date — some rows carry "غير محدد"
     * and those people have still left.
     */
    val resigned: Boolean get() = !resignationEnd.isNullOrBlank()

    companion object {
        private fun JSONObject.str(key: String): String? {
            if (isNull(key)) return null
            val v = optString(key, "")
            return v.ifBlank { null }
        }

        fun from(o: JSONObject) = Employee(
            id = o.str("id") ?: o.str("emp_code").orEmpty(),
            code = o.str("emp_code").orEmpty().trim(),
            nationalId = o.str("national_id"),
            nameAr = o.str("full_name"),
            nameEn = o.str("full_name_en"),
            jobTitle = o.str("job_title"),
            department = o.str("department"),
            category = o.str("category"),
            phone = o.str("phone"),
            hireDate = o.str("hire_date"),
            address = o.str("address"),
            permitDate = o.str("permit_date"),
            permitStatus = o.str("permit_status"),
            banReason = o.str("ban_reason"),
            resignationEnd = o.str("resignation_end_date"),
            notes = o.str("notes")
        )
    }
}

data class Stats(
    val active: Int,
    val resigned: Int,
    val expiring: Int,
    val banned: Int
)
