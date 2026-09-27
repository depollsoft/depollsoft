package depollsoft.lib.state

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * A StateList behaves as a plain list under any sequence of edits, and every edit that changes it
 * is a change a snapshot observer hears. Random operation sequences run against an ArrayList
 * reference; each is seeded, and a failure names its seed and step.
 */
class StateListPropertyTest {
    private fun Random.edit(
        list: MutableList<Int>,
        reference: MutableList<Int>,
        replace: (MutableList<Int>, Collection<Int>) -> Unit,
    ): String {
        val size = reference.size
        return when (nextInt(11)) {
            0 -> {
                val v = nextInt(50)
                list.add(v)
                reference.add(v)
                "add($v)"
            }
            1 -> {
                val i = nextInt(size + 1)
                val v = nextInt(50)
                list.add(i, v)
                reference.add(i, v)
                "add($i, $v)"
            }
            2 ->
                if (size == 0) {
                    "skip"
                } else {
                    val i = nextInt(size)
                    assertEquals(reference.removeAt(i), list.removeAt(i))
                    "removeAt($i)"
                }
            3 ->
                if (size == 0) {
                    "skip"
                } else {
                    val i = nextInt(size)
                    val v = nextInt(50)
                    assertEquals(reference.set(i, v), list.set(i, v))
                    "set($i, $v)"
                }
            4 -> {
                val v = nextInt(50)
                assertEquals(reference.remove(v), list.remove(v))
                "remove($v)"
            }
            5 -> {
                val batch = List(nextInt(4)) { nextInt(50) }
                list.addAll(batch)
                reference.addAll(batch)
                "addAll($batch)"
            }
            6 -> {
                val drop = List(nextInt(3)) { nextInt(50) }.toSet()
                assertEquals(reference.removeAll(drop), list.removeAll(drop))
                "removeAll($drop)"
            }
            7 -> {
                val keep = reference.shuffled(this).take(nextInt(size + 1))
                replace(list, keep)
                reference.clear()
                reference.addAll(keep)
                "replaceWith($keep)"
            }
            8 -> {
                // Replacing with itself, or a view of itself, keeps the items.
                val from = if (size == 0) 0 else nextInt(size)
                val view = list.subList(from, size)
                val expected = reference.subList(from, size).toList()
                replace(list, view)
                reference.clear()
                reference.addAll(expected)
                "replaceWith(subList($from))"
            }
            9 ->
                if (size < 2) {
                    "skip"
                } else {
                    // A drag's move: take one out, put it back elsewhere.
                    val from = nextInt(size)
                    val to = nextInt(size)
                    list.add(to, list.removeAt(from))
                    reference.add(to, reference.removeAt(from))
                    "move($from, $to)"
                }
            else -> {
                list.clear()
                reference.clear()
                "clear()"
            }
        }
    }

    @Test
    fun randomEditsMatchAPlainList() {
        repeat(300) { seed ->
            val random = Random(seed.toLong())
            val list = StateList<Int>()
            val reference = ArrayList<Int>()
            repeat(40) { step ->
                val op = random.edit(list, reference) { target, items -> (target as StateList<Int>).replaceWith(items) }
                assertEquals("seed $seed step $step after $op", reference, list.toList())
                assertEquals("seed $seed step $step size", reference.size, list.size)
                assertEquals("seed $seed step $step equality", reference, list)
            }
        }
    }

    @Test
    fun everyChangingEditIsHeard() {
        repeat(100) { seed ->
            val random = Random(10_000L + seed)
            val list = StateList<Int>()
            val reference = ArrayList<Int>()
            var heard = 0
            val handle = Snapshot.registerApplyObserver { changed, _ -> if (changed.isNotEmpty()) heard++ }
            try {
                repeat(30) { step ->
                    val before = reference.toList()
                    // A batched edit (replaceWith) applies as a snapshot and is heard at once; a
                    // plain one is heard when apply notifications go out.
                    heard = 0
                    val op = random.edit(list, reference) { target, items -> (target as StateList<Int>).replaceWith(items) }
                    Snapshot.sendApplyNotifications()
                    if (before != reference) assertTrue("seed ${10_000 + seed} step $step: $op changed the list unheard", heard > 0)
                }
            } finally {
                handle.dispose()
            }
        }
    }
}
