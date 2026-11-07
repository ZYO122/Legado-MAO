// SPDX-License-Identifier: Apache-2.0
// Geometry adapted from ciddwd/overlay-translator (64a7aa2).
package io.legado.app.data.repository.manga.translation

data class MangaShapeRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** A container hint classified by [kind]; produced locally or supplied by an upstream detector. */
data class MangaBubbleDetection(val bounds: MangaShapeRect, val confidence: Float, val kind: Int)

internal data class MangaInstanceMask(val left: Int, val top: Int, val width: Int, val height: Int, val pixels: BooleanArray) {
    init { require(width >= 0 && height >= 0 && pixels.size.toLong() == width.toLong() * height) }
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    fun contains(x: Int, y: Int): Boolean = x in left until right && y in top until bottom && pixels[(y - top) * width + x - left]
}
