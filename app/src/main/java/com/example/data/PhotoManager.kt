package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.GameColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class PhotoManager(private val context: Context) {

    private val photosDir: File = File(context.filesDir, "face_photos").apply {
        if (!exists()) mkdirs()
    }

    // Direct photos uploaded by user for each slot (null if none)
    private val rawSlotBitmaps = mutableMapOf<Int, Bitmap>()

    // Processed photos for each of the 6 colors after applying repetition rule
    private val _slotBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val slotBitmaps: StateFlow<Map<Int, Bitmap>> = _slotBitmaps.asStateFlow()

    private val _slotImageBitmaps = MutableStateFlow<Map<Int, ImageBitmap>>(emptyMap())
    val slotImageBitmaps: StateFlow<Map<Int, ImageBitmap>> = _slotImageBitmaps.asStateFlow()

    private val defaultAvatars = mutableMapOf<Int, Bitmap>()

    init {
        createDefaultAvatars()
        loadAllPhotos()
    }

    private fun getPhotoFile(colorId: Int): File = File(photosDir, "slot_$colorId.png")

    fun loadAllPhotos() {
        rawSlotBitmaps.clear()
        for (i in 0 until 6) {
            val file = getPhotoFile(i)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    rawSlotBitmaps[i] = bitmap
                }
            }
        }
        computeEffectiveBitmaps()
    }

    /**
     * Applies the PHOTO REPETITION RULE:
     * If user uploaded photos for fewer than 6 colors, distribute them across all 6 slots.
     * If 0 uploaded, fall back to default charming cartoon faces.
     */
    private fun computeEffectiveBitmaps() {
        val result = mutableMapOf<Int, Bitmap>()
        val uploadedList = rawSlotBitmaps.entries.sortedBy { it.key }.map { it.value }

        for (i in 0 until 6) {
            when {
                // If user uploaded for this specific slot, prefer it
                rawSlotBitmaps.containsKey(i) -> {
                    result[i] = rawSlotBitmaps[i]!!
                }
                // If user uploaded some photos elsewhere, repeat/distribute them!
                uploadedList.isNotEmpty() -> {
                    result[i] = uploadedList[i % uploadedList.size]
                }
                // No photos uploaded at all -> use default avatar
                else -> {
                    result[i] = defaultAvatars[i] ?: createSingleAvatar(i)
                }
            }
        }

        _slotBitmaps.value = result
        _slotImageBitmaps.value = result.mapValues { it.value.asImageBitmap() }
    }

    fun hasCustomPhoto(colorId: Int): Boolean = rawSlotBitmaps.containsKey(colorId)

    fun getDirectPhoto(colorId: Int): Bitmap? = rawSlotBitmaps[colorId]

    /**
     * Saves photo from Uri, center-crops to square and downscales to 256x256
     */
    fun savePhotoForSlot(colorId: Int, uri: Uri): Boolean {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return false

            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / (sampleSize * 2) >= 256) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return false

            val squareBitmap = cropToSquareAndScale(sampledBitmap, 256)
            val file = getPhotoFile(colorId)
            FileOutputStream(file).use { out ->
                squareBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            rawSlotBitmaps[colorId] = squareBitmap
            computeEffectiveBitmaps()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun removePhotoForSlot(colorId: Int) {
        val file = getPhotoFile(colorId)
        if (file.exists()) {
            file.delete()
        }
        rawSlotBitmaps.remove(colorId)
        computeEffectiveBitmaps()
    }

    private fun cropToSquareAndScale(src: Bitmap, targetSize: Int): Bitmap {
        val size = min(src.width, src.height)
        val xOffset = (src.width - size) / 2
        val yOffset = (src.height - size) / 2
        val cropped = Bitmap.createBitmap(src, xOffset, yOffset, size, size)
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (cropped != src && cropped != scaled) {
            cropped.recycle()
        }
        return scaled
    }

    private fun createDefaultAvatars() {
        for (i in 0 until 6) {
            defaultAvatars[i] = createSingleAvatar(i)
        }
    }

    /**
     * Generates a 256x256 high-resolution cartoon face bitmap for slot [colorId].
     * Distinct expressions for each color:
     * 0: Big smile
     * 1: Wink
     * 2: Star-eyed excited
     * 3: Cool sunglasses
     * 4: Cheerful tongue-out
     * 5: Warm blush smile
     */
    private fun createSingleAvatar(colorId: Int): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val skinColors = intArrayOf(
            0xFFFED7AA.toInt(), // Peach
            0xFFFDE68A.toInt(), // Warm Cream
            0xFFFECDD3.toInt(), // Light Rosy
            0xFFE2E8F0.toInt(), // Pale
            0xFFFED7AA.toInt(), // Peach
            0xFFFBCFE8.toInt()  // Soft Blush
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Face circle base
        paint.color = skinColors[colorId % skinColors.size]
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        // Inner shadow/contour for 3D depth
        paint.color = 0x1A000000
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 7, paint)

        // Features based on slot
        paint.style = Paint.Style.FILL
        val eyeColor = 0xFF1F2937.toInt()
        val eyeWhite = 0xFFFFFFFF.toInt()
        val mouthColor = 0xFF1F2937.toInt()

        when (colorId) {
            0 -> {
                // Happy big smile
                // Eyes
                paint.color = eyeColor
                canvas.drawCircle(84f, 100f, 14f, paint)
                canvas.drawCircle(172f, 100f, 14f, paint)
                paint.color = eyeWhite
                canvas.drawCircle(80f, 96f, 5f, paint)
                canvas.drawCircle(168f, 96f, 5f, paint)

                // Cheeks
                paint.color = 0x55F43F5E
                canvas.drawCircle(60f, 130f, 16f, paint)
                canvas.drawCircle(196f, 130f, 16f, paint)

                // Smile
                paint.color = mouthColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                paint.strokeCap = Paint.Cap.ROUND
                val path = Path().apply {
                    moveTo(80f, 145f)
                    quadTo(128f, 195f, 176f, 145f)
                }
                canvas.drawPath(path, paint)
            }
            1 -> {
                // Winking face
                // Left eye wink arc
                paint.color = eyeColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                paint.strokeCap = Paint.Cap.ROUND
                val leftEye = Path().apply {
                    moveTo(68f, 105f)
                    quadTo(84f, 85f, 100f, 105f)
                }
                canvas.drawPath(leftEye, paint)

                // Right eye open
                paint.style = Paint.Style.FILL
                canvas.drawCircle(172f, 100f, 14f, paint)
                paint.color = eyeWhite
                canvas.drawCircle(168f, 96f, 5f, paint)

                // Cheeks
                paint.color = 0x55F43F5E
                canvas.drawCircle(64f, 130f, 16f, paint)
                canvas.drawCircle(192f, 130f, 16f, paint)

                // Smile
                paint.color = mouthColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                val mouth = Path().apply {
                    moveTo(85f, 150f)
                    quadTo(128f, 195f, 171f, 150f)
                }
                canvas.drawPath(mouth, paint)
            }
            2 -> {
                // Excited smile
                // Eyes
                paint.color = eyeColor
                paint.style = Paint.Style.FILL
                canvas.drawCircle(84f, 96f, 15f, paint)
                canvas.drawCircle(172f, 96f, 15f, paint)
                paint.color = eyeWhite
                canvas.drawCircle(80f, 92f, 6f, paint)
                canvas.drawCircle(168f, 92f, 6f, paint)
                canvas.drawCircle(88f, 102f, 3f, paint)
                canvas.drawCircle(176f, 102f, 3f, paint)

                // Open happy mouth
                paint.color = 0xFFEF4444.toInt()
                paint.style = Paint.Style.FILL
                val openMouth = Path().apply {
                    moveTo(86f, 145f)
                    quadTo(128f, 145f, 170f, 145f)
                    quadTo(128f, 210f, 86f, 145f)
                }
                canvas.drawPath(openMouth, paint)
                paint.color = 0xFFFFFFFF.toInt()
                // Teeth
                canvas.drawRect(96f, 145f, 160f, 156f, paint)
            }
            3 -> {
                // Cool sunglasses face
                paint.color = 0xFF111827.toInt()
                paint.style = Paint.Style.FILL
                // Sunglasses lenses
                val leftLens = Path().apply {
                    addRoundRect(RectF(52f, 78f, 116f, 122f), 14f, 14f, Path.Direction.CW)
                }
                val rightLens = Path().apply {
                    addRoundRect(RectF(140f, 78f, 204f, 122f), 14f, 14f, Path.Direction.CW)
                }
                canvas.drawPath(leftLens, paint)
                canvas.drawPath(rightLens, paint)

                // Bridge and frame
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                canvas.drawLine(116f, 95f, 140f, 95f, paint)
                canvas.drawLine(40f, 90f, 54f, 95f, paint)
                canvas.drawLine(202f, 95f, 216f, 90f, paint)

                // White shine on glasses
                paint.color = 0x66FFFFFF
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                canvas.drawLine(64f, 86f, 82f, 114f, paint)
                canvas.drawLine(152f, 86f, 170f, 114f, paint)

                // Smirk smile
                paint.color = mouthColor
                paint.strokeWidth = 9f
                paint.strokeCap = Paint.Cap.ROUND
                val smirk = Path().apply {
                    moveTo(95f, 160f)
                    quadTo(135f, 185f, 165f, 155f)
                }
                canvas.drawPath(smirk, paint)
            }
            4 -> {
                // Playful tongue out
                paint.color = eyeColor
                paint.style = Paint.Style.FILL
                canvas.drawCircle(84f, 98f, 14f, paint)
                canvas.drawCircle(172f, 98f, 14f, paint)
                paint.color = eyeWhite
                canvas.drawCircle(80f, 94f, 5f, paint)
                canvas.drawCircle(168f, 94f, 5f, paint)

                // Tongue
                paint.color = 0xFFFB7185.toInt()
                val tongue = Path().apply {
                    moveTo(112f, 156f)
                    quadTo(128f, 206f, 144f, 156f)
                }
                canvas.drawPath(tongue, paint)

                // Mouth line
                paint.color = mouthColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 9f
                paint.strokeCap = Paint.Cap.ROUND
                val smile = Path().apply {
                    moveTo(85f, 150f)
                    quadTo(128f, 175f, 171f, 150f)
                }
                canvas.drawPath(smile, paint)
            }
            5 -> {
                // Sweet blushing smile
                // Smiling closed eyes (upside down arcs)
                paint.color = eyeColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 9f
                paint.strokeCap = Paint.Cap.ROUND
                val left = Path().apply {
                    moveTo(68f, 106f)
                    quadTo(84f, 88f, 100f, 106f)
                }
                val right = Path().apply {
                    moveTo(156f, 106f)
                    quadTo(172f, 88f, 188f, 106f)
                }
                canvas.drawPath(left, paint)
                canvas.drawPath(right, paint)

                // Bright rosy blush
                paint.color = 0x88F43F5E.toInt()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(60f, 126f, 18f, paint)
                canvas.drawCircle(196f, 126f, 18f, paint)

                // Cute smile
                paint.color = mouthColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 9f
                val cuteMouth = Path().apply {
                    moveTo(95f, 152f)
                    quadTo(128f, 182f, 161f, 152f)
                }
                canvas.drawPath(cuteMouth, paint)
            }
        }

        return bitmap
    }
}
