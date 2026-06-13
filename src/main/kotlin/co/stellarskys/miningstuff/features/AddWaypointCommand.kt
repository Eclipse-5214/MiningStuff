package co.stellarskys.miningstuff.features

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.api.handlers.Atlas

@Command
object AddWaypointCommand: Atlas("stella_internal_add_wp") {
    init {
        val x by arg.int()
        val y by arg.int()
        val z by arg.int()
        val areaName by arg.greedy()

        runs { LocationManager.addWaypoint(areaName, x.toDouble(), z.toDouble()) }
    }
}