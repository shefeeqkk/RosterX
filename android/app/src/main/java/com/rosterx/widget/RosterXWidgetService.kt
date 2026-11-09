package com.rosterx.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rosterx.R
import java.util.Locale

class RosterXWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RosterXWidgetFactory(applicationContext)
    }

    private sealed class WidgetRow {
        data class Header(val title: String, val count: String) : WidgetRow()
        data class ShiftPair(val left: WidgetDutyItem, val right: WidgetDutyItem?) : WidgetRow()
    }

    private class RosterXWidgetFactory(private val context: Context) : RemoteViewsFactory {

        private val rows = mutableListOf<WidgetRow>()
        private val gson = Gson()

        override fun onCreate() {
            loadItems()
        }

        override fun onDataSetChanged() {
            loadItems()
        }

        override fun onDestroy() {
            rows.clear()
        }

        override fun getCount(): Int = rows.size

        override fun getViewTypeCount(): Int = 2

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= rows.size) return null

            return when (val row = rows[position]) {
                is WidgetRow.Header -> {
                    val rv = RemoteViews(context.packageName, R.layout.item_widget_section_header)
                    rv.setTextViewText(R.id.widget_section_title, row.title)
                    rv.setTextViewText(R.id.widget_section_count, row.count)
                    rv
                }
                is WidgetRow.ShiftPair -> {
                    val rv = RemoteViews(context.packageName, R.layout.item_widget_shift_pair)

                    // 1. Column 1 (Left Engineer)
                    val left = row.left
                    rv.setTextViewText(R.id.left_badge_code, left.shiftCode)
                    if (!left.bgHex.isNullOrBlank() && left.bgHex.startsWith("#")) {
                        try {
                            rv.setInt(R.id.left_badge_bg, "setColorFilter", Color.parseColor(left.bgHex))
                        } catch (_: Exception) {}
                    }
                    if (!left.textHex.isNullOrBlank() && left.textHex.startsWith("#")) {
                        try {
                            rv.setTextColor(R.id.left_badge_code, Color.parseColor(left.textHex))
                        } catch (_: Exception) {}
                    }

                    rv.setTextViewText(R.id.left_engineer_name, extractFirstName(left.engineerName))
                    rv.setTextViewText(R.id.left_shift_timing, formatCompactTiming(left.timing.ifBlank { left.shiftName }))

                    if (left.isActiveNow) {
                        rv.setViewVisibility(R.id.left_active_dot, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.left_active_dot, View.GONE)
                    }

                    // 2. Column 2 (Right Engineer or Invisible Spacer)
                    val right = row.right
                    if (right != null) {
                        rv.setViewVisibility(R.id.pair_right_root, View.VISIBLE)
                        rv.setTextViewText(R.id.right_badge_code, right.shiftCode)
                        if (!right.bgHex.isNullOrBlank() && right.bgHex.startsWith("#")) {
                            try {
                                rv.setInt(R.id.right_badge_bg, "setColorFilter", Color.parseColor(right.bgHex))
                            } catch (_: Exception) {}
                        }
                        if (!right.textHex.isNullOrBlank() && right.textHex.startsWith("#")) {
                            try {
                                rv.setTextColor(R.id.right_badge_code, Color.parseColor(right.textHex))
                            } catch (_: Exception) {}
                        }

                        rv.setTextViewText(R.id.right_engineer_name, extractFirstName(right.engineerName))
                        rv.setTextViewText(R.id.right_shift_timing, formatCompactTiming(right.timing.ifBlank { right.shiftName }))

                        if (right.isActiveNow) {
                            rv.setViewVisibility(R.id.right_active_dot, View.VISIBLE)
                        } else {
                            rv.setViewVisibility(R.id.right_active_dot, View.GONE)
                        }
                    } else {
                        rv.setViewVisibility(R.id.pair_right_root, View.INVISIBLE)
                    }

                    rv
                }
            }
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true

        private fun loadItems() {
            rows.clear()
            val prefs = context.getSharedPreferences("rosterx_widget_cache", Context.MODE_PRIVATE)
            val json = prefs.getString("key_manager_duty_list_json", null)
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<WidgetDutyItem>>() {}.type
                    val rawList: List<WidgetDutyItem> = gson.fromJson(json, type) ?: emptyList()

                    var i = 0
                    while (i < rawList.size) {
                        val current = rawList[i]
                        if (current.itemType == WidgetDutyItem.TYPE_HEADER) {
                            rows.add(WidgetRow.Header(current.sectionTitle, current.sectionCount))
                            i++
                        } else {
                            // Group consecutive duty or rest items under this section into 2-column pairs
                            val left = current
                            val right = if (i + 1 < rawList.size && rawList[i + 1].itemType != WidgetDutyItem.TYPE_HEADER) {
                                rawList[i + 1]
                            } else {
                                null
                            }
                            rows.add(WidgetRow.ShiftPair(left, right))
                            i += if (right != null) 2 else 1
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        private fun extractFirstName(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return ""
            val first = trimmed.split(Regex("\\s+")).firstOrNull() ?: trimmed
            return first.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        private fun formatCompactTiming(timing: String): String {
            return timing
                .replace(" AM", "")
                .replace(" PM", "")
                .replace(" – ", " - ")
                .replace(" - ", " - ")
                .trim()
        }
    }
}
