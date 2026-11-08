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

class RosterXWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RosterXWidgetFactory(applicationContext)
    }

    private class RosterXWidgetFactory(private val context: Context) : RemoteViewsFactory {

        private val items = mutableListOf<WidgetDutyItem>()
        private val gson = Gson()

        override fun onCreate() {
            loadItems()
        }

        override fun onDataSetChanged() {
            loadItems()
        }

        override fun onDestroy() {
            items.clear()
        }

        override fun getCount(): Int = items.size

        override fun getViewTypeCount(): Int = 3

        override fun getViewAt(position: Int): RemoteViews? {
            if (position < 0 || position >= items.size) return null
            val item = items[position]

            return when (item.itemType) {
                WidgetDutyItem.TYPE_HEADER -> {
                    val rv = RemoteViews(context.packageName, R.layout.item_widget_section_header)
                    rv.setTextViewText(R.id.widget_section_title, item.sectionTitle)
                    rv.setTextViewText(R.id.widget_section_count, item.sectionCount)
                    rv
                }
                WidgetDutyItem.TYPE_REST -> {
                    val rv = RemoteViews(context.packageName, R.layout.item_widget_rest)
                    rv.setTextViewText(R.id.item_rest_badge_code, item.shiftCode)
                    try {
                        rv.setInt(R.id.item_rest_badge_bg, "setColorFilter", Color.parseColor(item.bgHex))
                        rv.setTextColor(R.id.item_rest_badge_code, Color.parseColor(item.textHex))
                    } catch (_: Exception) {}

                    rv.setTextViewText(R.id.item_rest_engineer_name, item.engineerName)
                    rv.setTextViewText(R.id.item_rest_desc, item.shiftName.ifBlank { "Scheduled Rest" })
                    rv
                }
                else -> {
                    val rv = RemoteViews(context.packageName, R.layout.item_widget_shift)
                    rv.setTextViewText(R.id.item_badge_code, item.shiftCode)
                    try {
                        rv.setInt(R.id.item_badge_bg, "setColorFilter", Color.parseColor(item.bgHex))
                        rv.setTextColor(R.id.item_badge_code, Color.parseColor(item.textHex))
                    } catch (_: Exception) {}

                    rv.setTextViewText(R.id.item_engineer_name, item.engineerName)
                    rv.setTextViewText(R.id.item_shift_timing, item.timing)

                    if (item.isActiveNow) {
                        rv.setViewVisibility(R.id.item_active_pill, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.item_active_pill, View.GONE)
                    }
                    rv
                }
            }
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true

        private fun loadItems() {
            items.clear()
            val prefs = context.getSharedPreferences("rosterx_widget_cache", Context.MODE_PRIVATE)
            val json = prefs.getString("key_manager_duty_list_json", null)
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<WidgetDutyItem>>() {}.type
                    val list: List<WidgetDutyItem> = gson.fromJson(json, type)
                    items.addAll(list)
                } catch (_: Exception) {}
            }
        }
    }
}

