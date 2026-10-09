package com.example.starborn.desktop

import androidx.compose.ui.geometry.*
import com.example.starborn.desktop.ui.*
import org.junit.Assert.*
import org.junit.Test

class DesktopHubLabelLayoutTest {
    private val image=Rect(300f,0f,800f,900f)
    private val viewport=Rect(0f,0f,1100f,900f)
    private fun intersects(a:Rect,b:Rect)=a.left<b.right && a.right>b.left && a.top<b.bottom && a.bottom>b.top
    @Test fun labelsAvoidArtworkAndEachOther() {
        val nodes=listOf(
            HubLabelInput("pit",Offset(475f,495f),Rect(420f,380f,530f,510f),Size(65f,44f)),
            HubLabelInput("astra",Offset(425f,594f),Rect(370f,500f,480f,625f),Size(80f,44f)),
            HubLabelInput("workshop",Offset(515f,711f),Rect(450f,600f,580f,740f),Size(125f,44f)))
        val result=layoutHubLabels(nodes,image,viewport,priorityId="pit")
        nodes.forEach { node -> nodes.forEach { art -> assertFalse("${node.id} covers ${art.id}",intersects(result.getValue(node.id).bounds,art.artwork!!)) } }
        val rects=result.values.map { it.bounds }
        for(i in rects.indices) for(j in i+1 until rects.size) assertFalse(intersects(rects[i],rects[j]))
        assertEquals(result,layoutHubLabels(nodes.reversed(),image,viewport,priorityId="pit"))
    }
    @Test fun crowdedMapUsesFreeBlurAndAvoidsHud() {
        val smallImage=Rect(400f,0f,620f,400f)
        val nodes=listOf(HubLabelInput("node",Offset(500f,200f),smallImage,Size(100f,56f)))
        val reserved=listOf(Rect(0f,0f,300f,400f),Rect(740f,0f,1100f,400f))
        val result=layoutHubLabels(nodes,smallImage,Rect(0f,0f,1100f,400f),reserved).getValue("node")
        assertFalse(intersects(result.bounds,smallImage))
        reserved.forEach { assertFalse(intersects(result.bounds,it)) }
        assertTrue(result.leader)
    }
    @Test fun edgeLabelsRemainInsideViewportAndMissingArtworkWorks() {
        val nodes=listOf(HubLabelInput("top",Offset(0f,0f),null,Size(120f,44f)),
            HubLabelInput("bottom",Offset(1100f,900f),null,Size(120f,56f)))
        layoutHubLabels(nodes,image,viewport).values.forEach {
            assertTrue(it.bounds.left>=0);assertTrue(it.bounds.top>=0)
            assertTrue(it.bounds.right<=1100);assertTrue(it.bounds.bottom<=900)
        }
    }
    @Test fun unavoidableCollisionsProduceDeterministicFallback() {
        val area=Rect(0f,0f,100f,100f)
        val nodes=listOf(HubLabelInput("node",Offset(50f,50f),area,Size(100f,56f)))
        val a=layoutHubLabels(nodes,area,area)
        assertEquals(a,layoutHubLabels(nodes,area,area))
        assertEquals(100f,a.getValue("node").bounds.width,.001f)
    }
}
