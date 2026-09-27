package depollsoft.tagmaster.barbershop

import depollsoft.lib.state.StateField

class TagQueryResult {
    var tags: List<Tag> by StateField(emptyList())
    var start: Int by StateField(0)
    var count: Int by StateField(0)
    var available: Int by StateField(0)
}
