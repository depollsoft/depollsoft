package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import android.view.View
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/** The detail pages now live under TagDetailFragment, so look through child managers too. */
private fun androidx.fragment.app.FragmentManager.allFragments(): List<androidx.fragment.app.Fragment> =
    fragments.flatMap { listOf(it) + it.childFragmentManager.allFragments() }

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SummaryLoadingBindingTest {
    @Test fun rating_and_sheet_completion_restore_real_bound_controls() = exercise(false)

    @Test fun rating_and_sheet_failure_restore_real_bound_controls() = exercise(true)

    private fun exercise(fail: Boolean) {
        val app = RuntimeEnvironment.getApplication()
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, app)
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val tag =
            spy(
                Tag().apply {
                    id = 2147483089
                    title = "Loading binding fixture"
                    parts = 4
                    sheetMusicUri =
                        RemoteLocation().apply {
                            uri = "https://example.invalid/compact.pdf"
                            type = "pdf"
                        }
                },
            )
        val rating = TaskCompletionSource<Boolean>()
        doReturn(rating.task).`when`(tag).rate(4)
        val controller =
            Robolectric.buildActivity(
                TagDetailActivity::class.java,
                Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id),
            )
        controller.get().setTheme(R.style.AppTheme)
        controller.get().tagLoader = { _, _ -> Task.forResult(tag) }
        try {
            controller.setup().visible()
            shadowOf(Looper.getMainLooper()).idle()
            val activity = controller.get()
            val summary =
                activity.supportFragmentManager
                    .allFragments()
                    .filterIsInstance<TagSummaryFragment>()
                    .single()
            val root = summary.requireView()
            val ratingPole = root.findViewById<BarberPoleLoadingView>(R.id.ratingSubmitProgress)
            val rateButton = root.findViewById<View>(R.id.rateButton)
            TagSummaryFragment::class.java
                .getDeclaredMethod("submitRating", View::class.java, Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(summary, root, 4)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(ratingPole.loading)
            assertFalse(rateButton.isEnabled)
            if (fail) rating.setError(IllegalStateException("Controlled failure")) else rating.setResult(true)
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(ratingPole.loading)
            assertEquals(fail, rateButton.isEnabled)
            assertFalse(ratingPole.isAnimating)
            val sheet = TaskCompletionSource<File>()
            mockConstruction(ContentCache::class.java) { cache, _ ->
                `when`(cache.loadContentPublic(anyString(), anyString(), anyBoolean())).thenReturn(sheet.task)
            }.use {
                val button = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.sheetMusicLink)
                val icon = button.icon
                val pole = root.findViewById<BarberPoleLoadingView>(R.id.sheetMusicProgress)
                button.performClick()
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue(pole.loading)
                assertFalse(button.isEnabled)
                assertSame(icon, button.icon)
                if (fail) {
                    sheet.setError(
                        IllegalStateException("Controlled failure"),
                    )
                } else {
                    sheet.setResult(File(app.cacheDir, "compact.pdf"))
                }
                shadowOf(Looper.getMainLooper()).idle()
                assertFalse(pole.loading)
                assertFalse(pole.isAnimating)
                assertTrue(button.isEnabled)
                assertSame(icon, button.icon)
                if (!fail) assertEquals(SheetMusicActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
