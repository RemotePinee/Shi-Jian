package com.eatwhat.util

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.eatwhat.data.model.GalleryImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PosterGenerator {
    suspend fun createPoster(context: Context, image: GalleryImage): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 1. Load the image using Coil for maximum robustness
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(image.localPath ?: image.url)
                .allowHardware(false) // Required for Canvas drawing
                .build()
            
            val result = loader.execute(request)
            if (result !is SuccessResult) return@withContext null
            val originalBitmap = result.drawable.toBitmap()
            
            // 2. Define Canvas Measurements (Fixed Width 1080)
            val width = 1080
            val padding = 80f
            val contentWidth = width - (padding * 2)
            
            // --- PRE-CALCULATE HEIGHTS & POSITIONS ---
            val imgRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val imgH = contentWidth / imgRatio
            
            // Title pre-calc
            val titlePaint = TextPaint().apply { color = Color.BLACK; textSize = 78f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val titleLayout = StaticLayout.Builder.obtain(image.recipeName, 0, image.recipeName.length, titlePaint, (contentWidth - 110f).toInt())
                .setLineSpacing(0f, 1.1f)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            
            val ingredientRows = (image.ingredients.size + 1) / 2
            val rowHeight = 65f
            val ingredientsSectionHeight = (ingredientRows * rowHeight) + 120f
            
            val cardTop = 280f
            val cardPadding = 55f
            val cardBottom = cardTop + imgH + 30f + titleLayout.height + 40f + ingredientsSectionHeight + cardPadding
            
            val footerHeight = 350f
            val posterHeight = (cardBottom + footerHeight).toInt()
            
            val poster = createBitmap(width, posterHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(poster)
            
            // 3. Background with Brutalist Grid
            canvas.drawColor("#FACC15".toColorInt())
            val gridPaint = Paint().apply { color = Color.BLACK; alpha = 24; style = Paint.Style.STROKE; strokeWidth = 3f }
            for (i in 0..width step 72) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), posterHeight.toFloat(), gridPaint)
            for (i in 0..posterHeight step 72) canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), gridPaint)
            
            // 4. --- MINIMALIST HEADER ---
            val headerPaint = TextPaint().apply { color = Color.BLACK; textSize = 48f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            canvas.drawText("食见 / AI VISUAL RECIPE", padding, 140f, headerPaint) // Moved down (was 100f)
            canvas.drawLine(padding, 165f, padding + 400f, 165f, Paint().apply { color = Color.BLACK; strokeWidth = 8f })
            
            // 5. --- UNIFIED MAIN CARD ---
            // Shadow (offset slightly more for better brutalist depth)
            canvas.drawRect(padding + 22f, cardTop + 22f, padding + contentWidth + 22f, cardBottom + 22f, Paint().apply { color = Color.BLACK })
            
            // Main Card Box (Fill)
            val cardRect = RectF(padding, cardTop, padding + contentWidth, cardBottom)
            canvas.drawRect(cardRect, Paint().apply { color = Color.WHITE })
            
            // 5.1 Processed Image Section (DRAW BEFORE STROKE to be framed by it)
            val imgRect = RectF(padding, cardTop, padding + contentWidth, cardTop + imgH)
            canvas.withClip(imgRect) {
                canvas.drawBitmap(originalBitmap, null, imgRect, null)
                
                // Image Processing Overlays (Technical Look)
                val overlayPaint = Paint().apply { color = Color.BLACK; alpha = 20; style = Paint.Style.FILL }
                for (y in cardTop.toInt()..(cardTop + imgH).toInt() step 12) {
                    canvas.drawRect(padding, y.toFloat(), padding + contentWidth, y + 2f, overlayPaint)
                }
                
                // Corner Brackets
                val bracketPaint = Paint().apply { color = Color.WHITE; strokeWidth = 6f; style = Paint.Style.STROKE; alpha = 180 }
                val bSize = 60f
                val bMargin = 30f
                canvas.drawLines(floatArrayOf(padding + bMargin, cardTop + bMargin + bSize, padding + bMargin, cardTop + bMargin, padding + bMargin, cardTop + bMargin, padding + bMargin + bSize, cardTop + bMargin), bracketPaint)
                canvas.drawLines(floatArrayOf(padding + contentWidth - bMargin, cardTop + imgH - bMargin - bSize, padding + contentWidth - bMargin, cardTop + imgH - bMargin, padding + contentWidth - bMargin, cardTop + imgH - bMargin, padding + contentWidth - bMargin - bSize, cardTop + imgH - bMargin), bracketPaint)
                
                // Metadata label
                val imgLabelPaint = TextPaint().apply { color = Color.WHITE; textSize = 22f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); alpha = 200 }
                canvas.drawText("SOURCE: AI_GENERATED_RENDER // SCALE 1:1", padding + 45f, cardTop + imgH - 45f, imgLabelPaint)
            }
            
            // Main Card Box (THICK STROKE DRAWN LAST for unified framing)
            canvas.drawRect(cardRect, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 14f })
            
            // 5.2 Dish Details Area
            canvas.withTranslation(padding + 55f, cardTop + imgH + 75f) {
                // Title
                titleLayout.draw(canvas)
                
                // Separator
                val lineY = titleLayout.height + 40f
                canvas.drawLine(-10f, lineY, contentWidth - 100f, lineY, Paint().apply { color = Color.BLACK; strokeWidth = 6f })
                
                // Ingredients
                val iLabelPaint = TextPaint().apply { color = Color.BLACK; textSize = 36f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); alpha = 180 }
                canvas.drawText("主要食材 | INGREDIENTS", 0f, lineY + 70f, iLabelPaint)
                
                var cY = lineY + 140f
                val iPaint = TextPaint().apply { color = Color.BLACK; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
                image.ingredients.chunked(2).forEach { pair ->
                    val line = pair.joinToString("   |   ") { it }
                    canvas.drawText("▪ $line", 5f, cY, iPaint)
                    cY += rowHeight
                }
            }
            
            // 6. --- FOOTER ALIGNMENT (FIXED SPACING) ---
            val footerBaseY = posterHeight - 180f
            canvas.drawLine(padding, footerBaseY - 35f, width - padding, footerBaseY - 35f, Paint().apply { color = Color.BLACK; strokeWidth = 6f })
            
            val prodPaint = TextPaint().apply { color = Color.BLACK; textSize = 38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            val datePaint = TextPaint().apply { color = Color.BLACK; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true; alpha = 140 }
            
            val fLeftY1 = footerBaseY + 50f
            val fLeftY2 = fLeftY1 + 50f
            canvas.drawText("Produced by 食见 AI", padding, fLeftY1, prodPaint)
            canvas.drawText("Generated at " + image.generatedAt, padding, fLeftY2, datePaint)
            
            // Right Side Barcode (Reduced height, shifted up)
            val barWands = listOf(4, 2, 7, 2, 4, 3, 9, 2, 6, 2, 4, 2, 8, 2, 4)
            val totalBW = barWands.sum() + (barWands.size - 1) * 6
            val spanTop = fLeftY1 - 34f
            
            val bcX = width - padding - totalBW - 8f
            val bcY = spanTop - 5f // Moved down slightly as requested (was -25f)
            val bcH = 50f 
            
            var curX = bcX
            val bPaint = Paint().apply { color = Color.BLACK; alpha = 240 }
            barWands.forEach { w ->
                canvas.drawRect(curX, bcY, curX + w, bcY + bcH, bPaint)
                curX += w + 6
            }
            
            val sPaint = TextPaint().apply { color = Color.BLACK; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); alpha = 130; textAlign = Paint.Align.CENTER }
            // Align "SAVOR & INSIGHT" exactly with date's baseline (fLeftY2)
            canvas.drawText("SAVOR & INSIGHT", bcX + totalBW / 2, fLeftY2, sPaint)
            
            return@withContext poster
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
