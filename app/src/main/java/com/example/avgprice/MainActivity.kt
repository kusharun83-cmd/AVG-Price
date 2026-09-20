package com.example.avgprice

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

// ---------- Colors ----------
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
private val BoughtBadgeBg = Color(0xFF16351F)
private val CalcBadgeBg = Color(0xFF2A2F38)

// ---------- Data model ----------
data class BuyEntry(var price: String = "", var qty: String = "")

data class StockSlot(
    var name: String = "",
    var entries: MutableList<BuyEntry> = mutableListOf(BuyEntry(), BuyEntry(), BuyEntry(), BuyEntry(), BuyEntry()),
    var isBought: Boolean = false,
    var ltp: String = ""
)

data class StockCalc(val totalQty: Double, val totalCost: Double, val avg: Double?)

private const val PREFS_NAME = "avgprice_prefs"
private const val KEY_SLOTS = "slots_json_v2"
private const val SLOT_COUNT = 10

private fun blankSlots(): MutableList<StockSlot> = MutableList(SLOT_COUNT) { StockSlot() }

private fun loadSlots(prefs: SharedPreferences): MutableList<StockSlot> {
    val raw = prefs.getString(KEY_SLOTS, null) ?: return blankSlots()
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<StockSlot>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val entriesArr = obj.optJSONArray("entries") ?: JSONArray()
            val entries = mutableListOf<BuyEntry>()
            for (j in 0 until entriesArr.length()) {
                val eo = entriesArr.getJSONObject(j)
                entries.add(BuyEntry(eo.optString("price", ""), eo.optString("qty", "")))
            }
            while (entries.size < 5) entries.add(BuyEntry())
            list.add(
                StockSlot(
                    name = obj.optString("name", ""),
                    entries = entries,
                    isBought = obj.optBoolean("isBought", false),
                    ltp = obj.optString("ltp", "")
                )
            )
        }
        while (list.size < SLOT_COUNT) list.add(StockSlot())
        list
    } catch (e: Exception) {
        blankSlots()
    }
}

private fun saveSlots(prefs: SharedPreferences, slots: List<StockSlot>) {
    val arr = JSONArray()
    slots.forEach { s ->
        val obj = JSONObject()
        obj.put("name", s.name)
        obj.put("isBought", s.isBought)
        obj.put("ltp", s.ltp)
        val entriesArr = JSONArray()
        s.entries.forEach { e ->
            val eo = JSONObject()
            eo.put("price", e.price)
            eo.put("qty", e.qty)
            entriesArr.put(eo)
        }
        obj.put("entries", entriesArr)
        arr.put(obj)
    }
    prefs.edit().putString(KEY_SLOTS, arr.toString()).apply()
}

private fun calcStock(slot: StockSlot): StockCalc {
    var totalQty = 0.0
    var totalCost = 0.0
    slot.entries.forEach { e ->
        val p = e.price.toDoubleOrNull()
        val q = e.qty.toDoubleOrNull()
        if (p != null && q != null && q > 0) {
            totalQty += q
            totalCost += p * q
        }
    }
    val avg = if (totalQty > 0) totalCost / totalQty else null
    return StockCalc(totalQty, totalCost, avg)
}

private fun inr(n: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return "\u20B9" + nf.format(n)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Surface)) {
                AvgPriceApp()
            }
        }
    }
}

@Composable
fun AvgPriceApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val slots = remember { mutableStateListOf<StockSlot>().apply { addAll(loadSlots(prefs)) } }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedSlot by remember { mutableStateOf(0) }

    fun updateSlot(index: Int, transform: (StockSlot) -> StockSlot) {
        slots[index] = transform(slots[index])
        saveSlots(prefs, slots)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("POSITION TRACKER", color = TextFaint, fontSize = 11.sp)
                Text("My Portfolio", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Bg,
                contentColor = Accent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Stocks") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Add / Edit") }
                )
            }

            when (selectedTab) {
                0 -> PortfolioTab(
                    slots = slots,
                    onSlotClick = { i ->
                        selectedSlot = i
                        selectedTab = 1
                    }
                )
                1 -> EditTab(
                    slotIndex = selectedSlot,
                    slot = slots[selectedSlot],
                    totalSlots = SLOT_COUNT,
                    filledFlags = slots.map { it.name.isNotBlank() },
                    onSlotIndexChange = { selectedSlot = it },
                    onUpdate = { transform -> updateSlot(selectedSlot, transform) },
                    onDone = { selectedTab = 0 },
                    onDelete = {
                        updateSlot(selectedSlot) { StockSlot() }
                        selectedTab = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun PortfolioTab(
    slots: List<StockSlot>,
    onSlotClick: (Int) -> Unit
) {
    var totalInvested = 0.0
    var totalPnl = 0.0
    var pnlBaseCount = 0
    var boughtCount = 0
    var calcCount = 0

    slots.forEach { s ->
        if (s.name.isNotBlank()) {
            val calc = calcStock(s)
            if (calc.avg != null && calc.totalQty > 0) {
                if (s.isBought) {
                    boughtCount++
                    totalInvested += calc.totalCost
                    val ltpVal = s.ltp.toDoubleOrNull()
                    if (ltpVal != null) {
                        totalPnl += (ltpVal - calc.avg) * calc.totalQty
                        pnlBaseCount++
                    }
                } else {
                    calcCount++
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface2)
                    .padding(18.dp)
            ) {
                Text("Total invested (bought only)", color = TextDim, fontSize = 12.sp)
                Text(
                    text = if (boughtCount > 0) inr(totalInvested) else "\u2014",
                    color = TextMain,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$boughtCount bought \u00B7 $calcCount calculating", color = TextDim, fontSize = 12.sp)
                    if (pnlBaseCount > 0) {
                        Text(
                            text = (if (totalPnl >= 0) "+" else "\u2212") + inr(Math.abs(totalPnl)),
                            color = if (totalPnl >= 0) Gain else Loss,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        itemsIndexed(slots) { index, slot ->
            SlotCard(
                index = index,
                slot = slot,
                onClick = { onSlotClick(index) }
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Made by Arun Kushwaha :)",
                color = TextFaint,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlotCard(index: Int, slot: StockSlot, onClick: () -> Unit) {
    val isEmpty = slot.name.isBlank()
    val calc = calcStock(slot)

    if (isEmpty) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Line), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+  Add stock #${index + 1}", color = TextFaint, fontSize = 13.sp)
        }
        return
    }

    val ltpVal = slot.ltp.toDoubleOrNull()
    val pnl = if (calc.avg != null && ltpVal != null && calc.totalQty > 0) (ltpVal - calc.avg) * calc.totalQty else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = slot.name,
                    color = TextMain,
                    fontSize = 15.sp,
                    fontWeight = if (slot.isBought) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (slot.isBought) BoughtBadgeBg else CalcBadgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (slot.isBought) "BOUGHT" else "CALC",
                        color = if (slot.isBought) Gain else TextDim,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (calc.avg != null)
                    "Avg ${inr(calc.avg)}  \u00B7  Qty ${calc.totalQty.toInt()}"
                else "No entries yet",
                color = TextDim,
                fontSize = 12.5.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Invested", color = TextFaint, fontSize = 10.5.sp)
            Text(
                text = if (calc.avg != null) inr(calc.totalCost) else "\u2014",
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (pnl != null) {
                Text(
                    text = (if (pnl >= 0) "+" else "\u2212") + inr(Math.abs(pnl)),
                    color = if (pnl >= 0) Gain else Loss,
                    fontSize = 11.5.sp
                )
            }
        }
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
private fun EditTab(
    slotIndex: Int,
    slot: StockSlot,
    totalSlots: Int,
    filledFlags: List<Boolean>,
    onSlotIndexChange: (Int) -> Unit,
    onUpdate: ((StockSlot) -> StockSlot) -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val calc = calcStock(slot)
    val ltpVal = slot.ltp.toDoubleOrNull()
    val pnl = if (calc.avg != null && ltpVal != null && calc.totalQty > 0) (ltpVal - calc.avg) * calc.totalQty else null
    val pnlPct = if (calc.avg != null && calc.avg != 0.0 && ltpVal != null) ((ltpVal - calc.avg) / calc.avg) * 100 else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
    ) {
        item {
            Text("Editing slot", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(totalSlots) { i ->
                    val filled = filledFlags.getOrElse(i) { false }
                    val selected = i == slotIndex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (selected) Accent else if (filled) Surface2 else Surface)
                            .border(
                                BorderStroke(1.dp, if (selected) Accent else Line),
                                CircleShape
                            )
                            .clickable { onSlotIndexChange(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            color = if (selected) Color.Black else if (filled) TextMain else TextFaint,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            OutlinedTextField(
                value = slot.name,
                onValueChange = { newName -> onUpdate { it.copy(name = newName) } },
                label = { Text("Stock name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (slot.isBought) "Marked as bought" else "Just calculating (trial)",
                        color = TextMain,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (slot.isBought)
                            "Counts toward your total invested amount"
                        else
                            "Won't count toward totals until you mark it bought",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = slot.isBought,
                    onCheckedChange = { checked -> onUpdate { it.copy(isBought = checked) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gain,
                        checkedTrackColor = GainBg,
                        uncheckedThumbColor = TextFaint,
                        uncheckedTrackColor = Surface
                    )
                )
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Buy entries", color = TextDim, fontSize = 13.sp)
                Text("${slot.entries.size} rows", color = TextFaint, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(slot.entries) { i, entry ->
            EditBuyRow(
                index = i,
                entry = entry,
                onChange = { newEntry ->
                    onUpdate { s ->
                        val newEntries = s.entries.toMutableList()
                        newEntries[i] = newEntry
                        s.copy(entries = newEntries)
                    }
                },
                onRemove = {
                    onUpdate { s ->
                        val newEntries = s.entries.toMutableList()
                        if (newEntries.size > 1) newEntries.removeAt(i)
                        s.copy(entries = newEntries)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            OutlinedButton(
                onClick = {
                    onUpdate { s ->
                        val newEntries = s.entries.toMutableList()
                        newEntries.add(BuyEntry())
                        s.copy(entries = newEntries)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim),
                border = BorderStroke(1.dp, Line)
            ) {
                Text("+  Add another buy")
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            OutlinedTextField(
                value = slot.ltp,
                onValueChange = { newLtp -> onUpdate { it.copy(ltp = newLtp) } },
                label = { Text("LTP (Last Traded Price)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Average price", color = TextDim, fontSize = 11.5.sp)
                        Text(
                            text = calc.avg?.let { inr(it) } ?: "\u2014",
                            color = TextMain,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Qty: ${if (calc.totalQty > 0) calc.totalQty.toInt().toString() else "0"}", color = TextDim, fontSize = 12.sp)
                        Text(
                            "Invested: ${if (calc.totalCost > 0) inr(calc.totalCost) else "\u2014"}",
                            color = TextDim,
                            fontSize = 12.sp
                        )
                    }
                }
                if (pnl != null && pnlPct != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (pnl >= 0) GainBg else LossBg)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("P&L", color = TextDim, fontSize = 12.sp)
                        Text(
                            text = (if (pnl >= 0) "+" else "\u2212") + inr(Math.abs(pnl)) +
                                "  (${if (pnlPct >= 0) "+" else "\u2212"}${"%.2f".format(Math.abs(pnlPct))}%)",
                            color = if (pnl >= 0) Gain else Loss,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
            ) {
                Text("Done \u2014 back to My Stocks", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear this stock slot", color = TextFaint, fontSize = 12.5.sp)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear slot ${slotIndex + 1}?") },
            text = { Text("This removes the stock name, all entries, and LTP from this slot.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Clear", color = Loss) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditBuyRow(
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
