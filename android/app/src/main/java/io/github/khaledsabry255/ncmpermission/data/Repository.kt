package io.github.khaledsabry255.ncmpermission.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Talks to Supabase over its REST endpoint. No SDK: one GET per query keeps
 * the dependency list — and the APK — small.
 */
class Repository(private val apiKey: String) {

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(query: String): JSONArray = withContext(Dispatchers.IO) {
        val url = URL("${Config.SUPABASE_URL}/rest/v1/employees?$query")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", apiKey)
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 20000
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            JSONArray(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(arr: JSONArray): List<Employee> {
        val seen = HashSet<String>()
        val out = ArrayList<Employee>(arr.length())
        for (i in 0 until arr.length()) {
            val emp = Employee.from(arr.getJSONObject(i))
            if (seen.add(emp.id)) out.add(emp)
        }
        return out
    }

    /** Sorts by employee code numerically, pushing unparsable codes to the end. */
    private fun byCode(a: Employee, b: Employee): Int {
        val x = Search.digitsOnly(a.code).toIntOrNull()
        val y = Search.digitsOnly(b.code).toIntOrNull()
        return when {
            x == null && y == null -> 0
            x == null -> 1
            y == null -> -1
            else -> x - y
        }
    }

    suspend fun search(term: String): List<Employee> {
        val raw = Search.sanitize(term)
        if (raw.isEmpty()) return emptyList()

        val filter = if (Search.isNumeric(raw)) {
            val d = Search.digitsOnly(raw)
            if (d.isEmpty()) return emptyList()
            // Numbers must match exactly — no substring hits.
            "or=" + enc("(emp_code.eq.$d,national_id.eq.$d)")
        } else {
            val p = Search.namePattern(raw) ?: return emptyList()
            "or=" + enc("(full_name.ilike.$p,full_name_en.ilike.$p)")
        }
        return parse(get("select=*&$filter&limit=${Config.MAX_RESULTS}"))
    }

    suspend fun banned(): List<Employee> =
        parse(get("select=*&permit_status=eq." + enc("ممنوع")))
            .filter { !it.resigned }
            .sortedWith(::byCode)

    suspend fun expiring(): List<Employee> =
        parse(get("select=*&permit_status=eq." + enc("ساري")))
            .filter { !it.resigned }
            .mapNotNull { emp -> Dates.daysLeft(emp.permitDate)?.let { emp to it } }
            .filter { it.second in 0..Config.EXPIRY_WINDOW }
            .sortedWith(compareBy({ it.second }, { Search.digitsOnly(it.first.code).toIntOrNull() ?: Int.MAX_VALUE }))
            .map { it.first }

    /**
     * Already past their date, for people still on staff — covers both rows
     * stored as "منتهي الصلاحية" and rows still marked "ساري" whose date has
     * passed, which would otherwise appear in no tab at all.
     */
    suspend fun expired(): List<Employee> {
        val filter = "or=" + enc("(permit_status.eq.ساري,permit_status.eq.منتهي الصلاحية)")
        return parse(get("select=*&$filter"))
            .filter { !it.resigned && Status.of(it).code == "EXPIRED" }
            // Most recently expired first, then by code — the page's order.
            .sortedWith(
                compareByDescending<Employee> { Dates.daysLeft(it.permitDate) ?: Int.MIN_VALUE }
                    .thenBy { Search.digitsOnly(it.code).toIntOrNull() ?: Int.MAX_VALUE }
            )
    }

    suspend fun stats(): Stats {
        val rows = parse(
            get("select=id,emp_code,national_id,permit_status,permit_date,resignation_end_date")
        )
        var active = 0; var resigned = 0; var expiring = 0; var banned = 0
        for (r in rows) {
            if (r.resigned) { resigned++; continue }
            active++
            when (r.permitStatus?.trim()) {
                "ممنوع" -> banned++
                "ساري" -> Dates.daysLeft(r.permitDate)?.let {
                    if (it in 0..Config.EXPIRY_WINDOW) expiring++
                }
            }
        }
        return Stats(active, resigned, expiring, banned)
    }
}
