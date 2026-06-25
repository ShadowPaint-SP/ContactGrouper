package de.drvlabs.contactgrouper.contacts

import android.content.Context
import coil.request.ImageRequest

fun Contact.photoModel(context: Context): ImageRequest? {
    val photo = photoUri ?: thumbnailUri ?: return null
    val cacheKey = "contact-photo-$id-${photoVersion ?: photo}"
    return ImageRequest.Builder(context)
        .data(photo)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .build()
}
