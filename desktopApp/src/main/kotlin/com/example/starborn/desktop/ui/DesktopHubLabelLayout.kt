package com.example.starborn.desktop.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

internal data class HubLabelInput(val id: String, val anchor: Offset, val artwork: Rect?, val size: Size)
internal data class HubLabelPlacement(val bounds: Rect, val leader: Boolean)

/** Deterministic placement in viewport pixels. Artwork and HUD bounds are occupied, not movable. */
internal fun layoutHubLabels(nodes: List<HubLabelInput>, image: Rect, viewport: Rect,
    reserved: List<Rect> = emptyList(), priorityId: String? = null, gap: Float = 6f, minimumTarget: Float = 0f): Map<String, HubLabelPlacement> {
    fun overlap(a: Rect, b: Rect): Float = ((minOf(a.right,b.right)-maxOf(a.left,b.left)).coerceAtLeast(0f) *
        (minOf(a.bottom,b.bottom)-maxOf(a.top,b.top)).coerceAtLeast(0f))
    fun padded(r: Rect) = Rect(r.left-gap,r.top-gap,r.right+gap,r.bottom+gap)
    val placed = linkedMapOf<String,HubLabelPlacement>()
    val obstacles = nodes.mapNotNull { it.artwork?.let(::padded) } + reserved.map(::padded)
    nodes.sortedWith(compareBy<HubLabelInput> { if(it.id==priorityId) 0 else 1 }.thenBy { it.anchor.y }.thenBy { it.id }).forEach { node ->
        val w=node.size.width.coerceAtMost(viewport.width); val h=node.size.height.coerceAtMost(viewport.height)
        val art=node.artwork ?: Rect(node.anchor,node.anchor)
        val below=Offset(node.anchor.x-w/2, maxOf(node.anchor.y,art.bottom)+gap)
        val above=Offset(node.anchor.x-w/2, art.top-gap-h)
        val left=Offset(art.left-gap-w,node.anchor.y-h/2)
        val right=Offset(art.right+gap,node.anchor.y-h/2)
        val candidates=mutableListOf(below,above,left,right)
        // Adjacent blur space is available for callouts when the map is crowded.
        for (dy in listOf(0f,-32f,32f,-64f,64f,-96f,96f,-128f,128f)) {
            candidates.add(Offset(image.left-gap-w,node.anchor.y-h/2+dy))
            candidates.add(Offset(image.right+gap,node.anchor.y-h/2+dy))
        }
        for (dy in listOf(-24f,24f,-48f,48f,-72f,72f)) candidates.add(below+Offset(0f,dy))
        val rects=candidates.map { p ->
            val x=p.x.coerceIn(viewport.left,(viewport.right-w).coerceAtLeast(viewport.left))
            val y=p.y.coerceIn(viewport.top,(viewport.bottom-h).coerceAtLeast(viewport.top))
            Rect(x,y,x+w,y+h)
        }.distinct()
        val occupied=obstacles+placed.values.map { padded(it.bounds) }
        fun penalty(r: Rect) = occupied.sumOf { overlap(r,it).toDouble() }
        fun routeCrossings(r: Rect): Int {
            val end=Offset(node.anchor.x.coerceIn(r.left,r.right),node.anchor.y.coerceIn(r.top,r.bottom))
            fun intersectsLine(box: Rect): Boolean {
                var near=0f; var far=1f
                for ((start,delta,low,high) in listOf(
                    listOf(node.anchor.x,end.x-node.anchor.x,box.left,box.right),
                    listOf(node.anchor.y,end.y-node.anchor.y,box.top,box.bottom))) {
                    if (delta==0f) { if(start<low || start>high) return false }
                    else {
                        val a=(low-start)/delta; val b=(high-start)/delta
                        near=maxOf(near,minOf(a,b));far=minOf(far,maxOf(a,b))
                        if(near>far) return false
                    }
                }
                return true
            }
            return (nodes.filter { it.id!=node.id }.mapNotNull { it.artwork }+reserved+placed.values.map { it.bounds })
                .count(::intersectsLine)
        }
        fun distance(r: Rect): Float {
            val dx=r.center.x-node.anchor.x; val dy=r.center.y-node.anchor.y
            fun target(box: Rect): Rect {
                val w=box.width.coerceAtLeast(minimumTarget);val h=box.height.coerceAtLeast(minimumTarget)
                return Rect(box.center.x-w/2,box.center.y-h/2,box.center.x+w/2,box.center.y+h/2)
            }
            val targetOverlap=placed.values.sumOf { overlap(target(r),target(it.bounds)).toDouble() }.toFloat()
            return dx*dx+dy*dy+targetOverlap*4f
        }
        val chosen=rects.take(2).firstOrNull { penalty(it)==0.0 && routeCrossings(it)==0 }
            ?: rects.filter { penalty(it)==0.0 }.minWithOrNull(compareBy<Rect>(::routeCrossings).thenBy(::distance))
            ?: rects.minWith(compareBy<Rect> { penalty(it) }.thenBy(::distance))
        placed[node.id]=HubLabelPlacement(chosen, chosen != rects.first())
    }
    return placed
}
