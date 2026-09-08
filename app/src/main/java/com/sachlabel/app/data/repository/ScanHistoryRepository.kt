package com.sachlabel.app.data.repository

import android.content.Context
import com.sachlabel.app.data.model.ProductScan
import com.sachlabel.app.data.model.Verdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SavedScanItem(
    val id: String,
    val brandName: String,
    val packInfo: String,
    val timestampFormatted: String,
    val timestampEpoch: Long,
    val frontClaim: String,
    val backTruth: String,
    val verdict: Verdict,
    val explanationEn: String,
    val isMock: Boolean
)

/**
 * Lightweight local persistence for product scan audits.
 * Saves history as a JSON file in internal storage — no heavy database or external backend needed.
 */
class ScanHistoryRepository(private val context: Context) {

    private val file = File(context.filesDir, "scan_history.json")
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _scans = MutableStateFlow<List<SavedScanItem>>(emptyList())
    val scans: StateFlow<List<SavedScanItem>> = _scans.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (!file.exists()) {
                _scans.value = emptyList()
                return
            }
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val items = mutableListOf<SavedScanItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    SavedScanItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        brandName = obj.optString("brandName", "Scanned Product"),
                        packInfo = obj.optString("packInfo", "Verified Pack"),
                        timestampFormatted = obj.optString("timestampFormatted", "Recently"),
                        timestampEpoch = obj.optLong("timestampEpoch", System.currentTimeMillis()),
                        frontClaim = obj.optString("frontClaim", ""),
                        backTruth = obj.optString("backTruth", ""),
                        verdict = Verdict.valueOf(obj.optString("verdict", Verdict.NOT_ENOUGH_EVIDENCE.name)),
                        explanationEn = obj.optString("explanationEn", ""),
                        isMock = obj.optBoolean("isMock", false)
                    )
                )
            }
            _scans.value = items.sortedByDescending { it.timestampEpoch }
        } catch (e: Exception) {
            _scans.value = emptyList()
        }
    }

    fun addScan(scan: ProductScan) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(now))

        val inferredBrand = scan.structuredLabel?.frontClaimsRaw?.firstOrNull()
            ?.take(32) ?: "Scanned Product"

        val item = SavedScanItem(
            id = scan.id,
            brandName = inferredBrand,
            packInfo = "Pack Audit",
            timestampFormatted = formattedDate,
            timestampEpoch = now,
            frontClaim = scan.result?.frontText ?: "",
            backTruth = scan.result?.evidence?.quote?.ifBlank { "No contradiction found" } ?: "No contradiction found",
            verdict = scan.result?.verdict ?: Verdict.NOT_ENOUGH_EVIDENCE,
            explanationEn = scan.result?.explanationEn ?: "",
            isMock = scan.isMock
        )

        val updated = listOf(item) + _scans.value
        _scans.value = updated

        scope.launch {
            saveToDisk(updated)
        }
    }

    private fun saveToDisk(items: List<SavedScanItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("brandName", item.brandName)
                obj.put("packInfo", item.packInfo)
                obj.put("timestampFormatted", item.timestampFormatted)
                obj.put("timestampEpoch", item.timestampEpoch)
                obj.put("frontClaim", item.frontClaim)
                obj.put("backTruth", item.backTruth)
                obj.put("verdict", item.verdict.name)
                obj.put("explanationEn", item.explanationEn)
                obj.put("isMock", item.isMock)
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun clearHistory() {
        _scans.value = emptyList()
        scope.launch {
            if (file.exists()) file.delete()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ScanHistoryRepository? = null

        fun getInstance(context: Context): ScanHistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScanHistoryRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
