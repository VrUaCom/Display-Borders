package com.viktor.edgeguard

import android.content.Context

object Prefs {
    private const val FILE = "edge_guard_prefs_v2"
    private const val SIDE = "side_border_px"
    private const val VERTICAL = "vertical_border_px"

    private fun p(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun side(context: Context) = p(context).getInt(SIDE, 24)
    fun vertical(context: Context) = p(context).getInt(VERTICAL, 0)

    fun setSide(context: Context, value: Int) = p(context).edit().putInt(SIDE, value).apply()
    fun setVertical(context: Context, value: Int) = p(context).edit().putInt(VERTICAL, value).apply()
}
