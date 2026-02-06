package ru.netology.nmedia.util

import android.os.Bundle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object StringArg : ReadWriteProperty<Bundle, String?> {
    override fun getValue(thisRef: Bundle, property: KProperty<*>): String? =
        thisRef.getString(property.name)

    override fun setValue(thisRef: Bundle, property: KProperty<*>, value: String?) {
        thisRef.putString(property.name, value)
    }

}

object LongArg : ReadWriteProperty<Bundle, Long?> {
    override fun getValue(
        thisRef: Bundle,
        property: KProperty<*>
    ): Long =
        thisRef.getLong(property.name)


    override fun setValue(
        thisRef: Bundle,
        property: KProperty<*>,
        value: Long?
    ) {
        thisRef.putLong(property.name, value ?: -1)
    }

}