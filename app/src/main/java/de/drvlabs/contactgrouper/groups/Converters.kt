package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromString(value: String?): Uri? {
        return value?.toUri()
    }

    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun fromIntToList(value: String): List<Long> {
        if (value.isEmpty()) {
            return emptyList()
        }
        return value.split(",").map { it.toLong() }
    }

    @TypeConverter
    fun fromListToInt(list: List<Long>): String {
        if (list.isEmpty()) {
            return ""
        }
        return list.joinToString(",")
    }
}
