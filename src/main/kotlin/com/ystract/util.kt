package com.ystract

import com.google.gson.internal.LinkedTreeMap


fun LinkedTreeMap<*, *>.getMap(key: String): LinkedTreeMap<*, *> {
    try {
        return this[key] as LinkedTreeMap<*, *>
    } catch (e: Exception) {
    }
    throw Exception("null key $key")
}

fun LinkedTreeMap<*, *>.getList(key: String): List<*> {
    try {
        return this[key] as List<*>
    } catch (e: Exception) {
    }
    throw Exception("null key $key")
}

fun List<*>.getMap(index: Int): LinkedTreeMap<*, *> {
    try {
        return this[index] as LinkedTreeMap<*, *>
    } catch (e: Exception) {
    }
    throw Exception("null index $index")
}
