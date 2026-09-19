package com.example.avgprice

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

// ---------- Colors (dark finance-app palette, matches the web version) ----------
private val Bg = Color(0xFF0E1013)
private val Surface = Color(0xFF171A1F)
private val Surface2 = Color(0xFF1E222A)
private val Line = Color(0xFF2A2F38)
private val TextMain = Color(0xFFECEEF1)
private val TextDim = Color(0xFF8B93A1)
private val TextFaint = Color(0xFF565D6B)
private val Accent = Color(0xFF5B8CFF)
private val Gain = Color(0xFF23C77E)
private val GainBg = Color(0xFF133023)
private val Loss = Color(0xFFF1554C)
private val LossBg = Color(0xFF331A1A)

data class BuyEntry(var price: String = "", var qty: String = "")

private const val PREFS_NAME = "avgprice_prefs"
private const val KEY_ENTRIES = "entries_json"
private const val KEY_LTP = "ltp"

private fun loadEntries(prefs: SharedPreferences): MutableList<BuyEntry> {
    val raw = prefs.getString(KEY_ENTRIES, null)
    if (raw.isNullOrBlank()) {
        return MutableList(5) { BuyEntry() } // 5 blank rows by default
    }
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<BuyEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(BuyEntry(obj.optString("price", ""), obj.optString("qty", "")))
        }
        if (list.isEmpty()) MutableList(5) { BuyEntry() } else list
    } catch (e: Exception) {
        MutableList(5) { BuyEntry() }
    }
}

private fun saveEntries(prefs: SharedPreferences, entries: List<BuyEntry>, ltp: String) {
    val arr = JSONArray()
    entries.forEach {
        val obj = JSONObject()
        obj.put("price", it.price)
        obj.put("qty", it.qty)
        arr.put(obj)
    }
    prefs.edit().putString(KEY_ENTRIES, arr.toString()).putString(KEY_LTP, ltp).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Surface)) {
                AvgPriceScreen()
            }
        }
    }
}

private fun inr(n: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return "\u20B9" + nf.format(n)
}

@Composable
fun AvgPriceScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val entries = remember { mutableStateListOf<BuyEntry>().apply { addAll(loadEntries(prefs)) } }
    var ltp by rememberSaveable { mutableStateOf(prefs.getString(KEY_LTP, "") ?: "") }
    var showResetDialog by remember { mutableStateOf(false) }

    fun persist() = saveEntries(prefs, entries, ltp)

    // Derived calculations
    var totalQty = 0.0
    var totalCost = 0.0
    entries.forEach { e ->
        val p = e.price.toDoubleOrNull()
        val q = e.qty.toDoubleOrNull()
        if (p != null && q != null && q > 0) {
            totalQty += q
            totalCost += p * q
        }
    }
    val avg = if (totalQty > 0) totalCost / totalQty else null
    val ltpVal = ltp.toDoubleOrNull()
    val pnl = if (avg != null && ltpVal != null && totalQty > 0) (ltpVal - avg) * totalQty else null
    val pnlPct = if (avg != null && avg != 0.0 && ltpVal != null) ((ltpVal - avg) / avg) * 100 else null

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("POSITION TRACKER", color = TextFaint, fontSize = 11.sp)
                    Text("Average Price", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Summary card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface2)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Average buy price", color = TextDim, fontSize = 12.sp)
                        Text(
                            text = avg?.let { inr(it) } ?: "\u2014",
                            color = TextMain,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Qty: ${if (totalQty > 0) totalQty.toInt().toString() else "0"}", color = TextDim, fontSize = 13.sp)
                        Text(
                            "Invested: ${if (totalCost > 0) inr(totalCost) else "\u2014"}",
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Divider(color = Line, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = ltp,
                    onValueChange = { ltp = it; persist() },
                    label = { Text("LTP (Last Traded Price)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Spacer(Modifier.height(12.dp))

                val pnlBg = when {
                    pnl == null -> Surface
                    pnl >= 0 -> GainBg
                    else -> LossBg
                }
                val pnlColor = when {
                    pnl == null -> TextMain
                    pnl >= 0 -> Gain
                    else -> Loss
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(pnlBg)
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("P&L", color = TextDim, fontSize = 12.5.sp)
                    Text(
                        text = if (pnl != null && pnlPct != null) {
                            val sign = if (pnl >= 0) "+" else "\u2212"
                            "$sign${inr(Math.abs(pnl))}  (${if (pnlPct >= 0) "+" else "\u2212"}${"%.2f".format(Math.abs(pnlPct))}%)"
                        } else "\u2014",
                        color = pnlColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Buy entries", color = TextDim, fontSize = 13.sp)
                Text("${entries.size} row${if (entries.size == 1) "" else "s"}", color = TextFaint, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    BuyRow(
                        index = index,
                        entry = entry,
                        onChange = { newEntry ->
                            entries[index] = newEntry
                            persist()
                        },
                        onRemove = {
                            entries.removeAt(index)
                            persist()
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    entries.add(BuyEntry())
                    persist()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim),
                border = BorderStroke(1.dp, Line)
            ) {
                Text("+  Add another buy")
            }

            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear all entries", color = TextFaint, fontSize = 12.5.sp)
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear all entries?") },
            text = { Text("This removes every buy entry and the LTP value.") },
            confirmButton = {
                TextButton(onClick = {
                    entries.clear()
                    entries.addAll(List(5) { BuyEntry() })
                    ltp = ""
                    persist()
                    showResetDialog = false
                }) { Text("Clear", color = Loss) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextDim,
    cursorColor = Accent
)

@Composable
private fun BuyRow(
    index: Int,
    entry: BuyEntry,
    onChange: (BuyEntry) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${index + 1}",
            color = TextFaint,
            fontSize = 12.sp,
            modifier = Modifier.width(18.dp)
        )
        OutlinedTextField(
            value = entry.price,
            onValueChange = { onChange(entry.copy(price = it)) },
            label = { Text("Price", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = fieldColors()
        )
        OutlinedTextField(
            value = entry.qty,
            onValueChange = { onChange(entry.copy(qty = it)) },
            label = { Text("Qty", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = fieldColors()
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Text("\u00D7", color = TextFaint, fontSize = 18.sp)
        }
    }
}

// (uses the standard androidx.compose.foundation.lazy.itemsIndexed and
//  androidx.compose.ui.draw.clip imported above)

