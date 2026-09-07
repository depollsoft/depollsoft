package depollsoft.tagmaster

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The links Tag Master claims, and what it does with the ones it does not.
 *
 * Valid catalog links keep their routing. Invalid IDs never launch detail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
class DeepLinkContractTest {
    @Test
    fun tag_links_are_still_handled() {
        assertTrue(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=31"),
            ),
        )
        assertTrue(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=4085"),
            ),
        )
    }

    @Test
    fun invalid_ids_are_not_claimed_or_sent_to_detail() {
        for (id in listOf("", "0", "-1", "2147483648", "99999999999999999", "abc", "1.5")) {
            val uri = Uri.parse("http://barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=$id")
            assertFalse("Invalid ID: $id", UrlHandlerActivity.canHandleUri(uri))
            val activity =
                Robolectric
                    .buildActivity(
                        UrlHandlerActivity::class.java,
                        Intent(Intent.ACTION_VIEW, uri),
                    ).create()
                    .get()
            assertEquals(
                TagMasterBrowserActivity::class.java.name,
                shadowOf(activity).nextStartedActivity.component!!.className,
            )
        }
    }

    @Test
    fun positive_integer_boundaries_remain_valid() {
        for (id in listOf(1, Int.MAX_VALUE)) {
            assertTrue(
                UrlHandlerActivity.canHandleUri(
                    Uri.parse("http://barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=$id"),
                ),
            )
        }
    }

    @Test
    fun non_tag_links_are_not_claimed() {
        assertFalse(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://www.barbershoptags.com/dbpage.php?pg=list&dbase=tags&id=31"),
            ),
        )
        assertFalse(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://www.barbershoptags.com/somewhere-else?pg=view&dbase=tags&id=31"),
            ),
        )
        assertFalse(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://example.com/dbpage.php?pg=view&dbase=tags&id=31"),
            ),
        )
    }

    @Test
    fun malformed_links_answer_no_rather_than_throwing() {
        // Each of these used to dereference a null query parameter or host.
        assertFalse(UrlHandlerActivity.canHandleUri(null))
        assertFalse(UrlHandlerActivity.canHandleUri(Uri.parse("")))
        assertFalse(UrlHandlerActivity.canHandleUri(Uri.parse("mailto:singer@example.com")))
        assertFalse(
            UrlHandlerActivity.canHandleUri(Uri.parse("http://barbershoptags.com/dbpage.php")),
        )
        assertFalse(
            UrlHandlerActivity.canHandleUri(
                Uri.parse("http://barbershoptags.com/dbpage.php?pg=view&dbase=tags"),
            ),
        )
    }

    @Test
    fun a_tag_link_opens_that_tag() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=31"),
            )
        val activity = Robolectric.buildActivity(UrlHandlerActivity::class.java, intent).create().get()

        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(
            TagDetailActivity::class.java.name,
            started.component!!.className,
        )
        assertEquals(31, started.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
    }

    @Test
    fun another_barbershoptags_page_still_opens_in_the_in_app_browser() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.barbershoptags.com/dbpage.php?pg=list&dbase=tags"),
            )
        val activity = Robolectric.buildActivity(UrlHandlerActivity::class.java, intent).create().get()

        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(
            TagMasterBrowserActivity::class.java.name,
            started.component!!.className,
        )
    }

    @Test
    fun a_view_intent_with_no_data_lands_on_the_desk() {
        val intent = Intent(Intent.ACTION_VIEW)
        val activity = Robolectric.buildActivity(UrlHandlerActivity::class.java, intent).create().get()

        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(MeActivity::class.java.name, started.component!!.className)
    }
}
