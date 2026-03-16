package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.compose.ui.graphics.Color
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
    fun colorToLong(color: Color): Long {
        return color.value.toLong()
    }

    @TypeConverter
    fun longToColor(value: Long): Color {
        return Color(value.toULong())
    }

    @TypeConverter
    fun groupSyncSourceToString(source: GroupSyncSource): String {
        return source.name
    }

    @TypeConverter
    fun stringToGroupSyncSource(value: String): GroupSyncSource {
        return GroupSyncSource.valueOf(value)
    }
}
