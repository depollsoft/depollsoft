package depollsoft.tagmaster

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.view.Menu
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.utils.WeakReflectedProperty
import com.bindroid.utils.bind
import com.bindroid.utils.compiledProp
import com.bindroid.utils.uibind
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SheetMusicActivity : AppCompatActivity() {
    var tag: Tag? by trackable()
    var drawable: Drawable? by trackable {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            photoView?.setImageDrawable(it)
        }
    }
    var rotation: Float by trackable(0f) {
        loadImage()
    }
    lateinit var photoView: PhotoView
    lateinit var keyButton: ExtendedFloatingActionButton

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sheetmusicview)
        enableDeskBack()
        findViewById<android.view.View>(R.id.sheetMusicRoot).applyDeskInsets()
        val tagId = intent.getIntExtra("tagId", -1)
        if (tagId <= 0) {
            finish()
            return
        }
        Tag.loadTagById(tagId).onSuccess {
            tag = it.result
        }

        photoView = findViewById(R.id.photoView)
        photoView.keepScreenOn = SettingsModel.wakeLockOnSheetMusic

        keyButton = findViewById(R.id.keyButton)
        keyButton.extend()
        uibind(
            R.id.keyButton,
            "Visibility",
            compiledProp { tag!!::keyNote },
            converter = BoolConverter.get(),
        )
        uibind(R.id.keyButton, "Text", { tag!!::writtenKey })
        // Press and hold sounds the key; lifting, cancelling, or the gesture
        // being taken over by a scroll all stop it, so a note can never be left
        // sounding under the sheet music.
        keyButton.setOnTouchListener { _, event ->
            val note = tag?.keyNote
            when (event.action) {
                MotionEvent.ACTION_DOWN -> note?.play()

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE,
                -> note?.stop()
            }
            false
        }

        bind(WeakReflectedProperty(this, "Title"), compiledProp { tag!!::title })

        loadImage()
    }

    override fun onStop() {
        super.onStop()
        tag?.keyNote?.stop()
    }

    override fun onPause() {
        super.onPause()
        tag?.keyNote?.stop()
    }

    private fun rotate() {
        rotation = (rotation + 90f) % 360f
    }

    private fun appendBitmap(
        first: Bitmap,
        second: Bitmap,
    ): Bitmap {
        val result =
            Bitmap.createBitmap(
                Math.max(first.width, second.width),
                first.height + second.height,
                Bitmap.Config.ARGB_8888,
            )
        val c = Canvas(result)
        c.drawBitmap(first, ((c.width - first.width) / 2).toFloat(), 0f, null)
        c.drawBitmap(second, ((c.width - second.width) / 2).toFloat(), first.height.toFloat(), null)
        return result
    }

    private fun rotateBitmap(
        bmp: Bitmap,
        rotation: Float,
    ): Bitmap {
        if (rotation == 0f) {
            return bmp
        }
        val matrix = Matrix()
        matrix.postRotate(rotation)
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    private val loadMutex = Mutex()

    private fun loadImage() {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            loadMutex.withLock {
                var failed = false
                var bitmap: Bitmap? = null
                try {
                    if (intent.type == "application/pdf") {
                        val fd = contentResolver.openFileDescriptor(intent.data!!, "r")
                        val renderer = PdfRenderer(fd!!)
                        val dpi = Math.min(resources.displayMetrics.densityDpi, 200)
                        for (pageIndex in 0 until renderer.pageCount) {
                            val page = renderer.openPage(pageIndex)
                            val width = dpi * page.width / 72
                            val height = dpi * page.height / 72
                            var pageBitmap =
                                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            pageBitmap.eraseColor(Color.WHITE)
                            page.render(
                                pageBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            page.close()
                            pageBitmap = rotateBitmap(pageBitmap, rotation)
                            if (bitmap == null) {
                                bitmap = pageBitmap
                            } else {
                                bitmap = appendBitmap(bitmap, pageBitmap)
                            }
                        }
                        renderer.close()
                    } else {
                        val stream = contentResolver.openInputStream(intent.data!!)
                        bitmap = rotateBitmap(BitmapFactory.decodeStream(stream), rotation)
                        stream?.close()
                    }
                } catch (e: Exception) {
                    failed = true
                }
                if (failed || bitmap!!.byteCount > MAX_BITMAP_SIZE) {
                    launch(Dispatchers.Main) {
                        Toast
                            .makeText(
                                this@SheetMusicActivity,
                                "Sheet music could not open in Tag Master -- opening in external app",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                    // The image is too big!  Bail out to an app that might have better luck
                    val toLaunch = Intent(intent)
                    toLaunch.component = null
                    toLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(toLaunch)
                    finish()
                } else {
                    drawable = bitmap.toDrawable(resources)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sheetmusicmenu, menu)
        menu.findItem(R.id.rotateButton)?.setOnMenuItemClickListener {
            rotate()
            true
        }
        menu.findItem(R.id.launchButton)?.setOnMenuItemClickListener {
            val toLaunch = Intent(intent)
            toLaunch.component = null
            startActivity(toLaunch)
            true
        }
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        const val MAX_BITMAP_SIZE = 1024 * 1024 * 100 // 100MiB
    }
}
