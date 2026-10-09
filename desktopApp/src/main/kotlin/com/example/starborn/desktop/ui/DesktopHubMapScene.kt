package com.example.starborn.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.hub.presentation.*
import com.example.starborn.feature.hub.viewmodel.*
import kotlin.math.roundToInt
import kotlin.math.sin

/** All anchors are fractions of the fitted original image, never of a padded map panel. */
internal fun hubAnchor(width: Float, height: Float, x: Float, y: Float) = Offset(width * x, height * y)

private data class HubNodeVisual(val node: HubNodeUi, val painter: androidx.compose.ui.graphics.painter.Painter,
    val anchor: Offset, val art: androidx.compose.ui.geometry.Rect?, val imageSize: Float, val occupiedArt: androidx.compose.ui.geometry.Rect?,
    val labelSize: androidx.compose.ui.geometry.Size, val truncated: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopHubMapScene(services: DesktopAppServices, state: HubUiState,
    accent: Color, onSelect: (String) -> Unit, onEnter: (HubNodeUi) -> Unit,
    imageBounds: androidx.compose.ui.geometry.Rect? = null, reservedBounds: List<androidx.compose.ui.geometry.Rect> = emptyList(),
    compact: Boolean = false) {
    val layout = HubMapLayouts.all[state.hub?.id]
    val settings = LocalExplorationSettings.current
    val motion = !settings.disableScreenshake && !settings.disableFlashes
    val transition = rememberInfiniteTransition(label = "hub-map")
    val phase by transition.animateFloat(0f, 6.283185f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "hub-map-phase")
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall.copy(fontSize = if(compact) 11.sp else 12.sp,
        lineHeight = if(compact) 13.sp else 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val viewport = androidx.compose.ui.geometry.Rect(0f,0f,constraints.maxWidth.toFloat(),constraints.maxHeight.toFloat())
        val image = imageBounds ?: viewport
        val cap = with(density) { (if(compact) 104.dp else 128.dp).toPx() }
        val paddingX = with(density) { 14.dp.toPx() }
        val paddingY = with(density) { 8.dp.toPx() }
        val iconSpace = with(density) { 16.dp.toPx() }
        val minTarget = with(density) { (if(settings.largeTouchTargets) 56.dp else 44.dp).toPx() }
        val visuals = state.nodes.map { node ->
            val site = if (node.id == "astra_access") layout?.astraDock else layout?.sites?.get(node.id)
            val synthetic = node.id == "astra_access" || node.id == "astra_disembark"
            val anchor = image.topLeft + hubAnchor(image.width,image.height,
                if(synthetic) site?.x ?: node.centerX else node.centerX,
                if(synthetic) site?.y ?: node.centerY else node.centerY)
            val painter = rememberDesktopAssetPainter(if(node.id=="astra_access") "images/nodes/astra_ship_map_v2.webp" else node.iconPath,services.assetProvider)
            val imageSize = if(painter is DesktopBitmapPainter && (site?.artworkWidth ?: .25f)>0f) image.width*(site?.artworkWidth ?: .25f) else 0f
            val ratio = if(node.id=="astra_access") .79f else .94f
            val art = if(imageSize>0) androidx.compose.ui.geometry.Rect(anchor.x-imageSize/2,anchor.y-imageSize*ratio,
                anchor.x+imageSize/2,anchor.y+imageSize*(1-ratio)) else null
            val occupiedArt = art?.let { box ->
                val intrinsic=painter.intrinsicSize
                val scale=minOf(imageSize/intrinsic.width,imageSize/intrinsic.height)
                val fittedWidth=intrinsic.width*scale;val fittedHeight=intrinsic.height*scale
                androidx.compose.ui.geometry.Rect(box.center.x-fittedWidth/2,box.center.y-fittedHeight/2,
                    box.center.x+fittedWidth/2,box.center.y+fittedHeight/2)
            }
            val statusSpace = if(!node.canEnter || node.completed) iconSpace else 0f
            val measurement = measurer.measure(androidx.compose.ui.text.AnnotatedString(node.title), labelStyle,
                maxLines=2, overflow=TextOverflow.Ellipsis,
                constraints=androidx.compose.ui.unit.Constraints(maxWidth=(cap-paddingX-statusSpace).toInt().coerceAtLeast(1)))
            HubNodeVisual(node,painter,anchor,art,imageSize,occupiedArt,
                androidx.compose.ui.geometry.Size(measurement.size.width+paddingX+statusSpace,measurement.size.height+paddingY),measurement.hasVisualOverflow)
        }
        // Selection changes styling only. Preserve this arrangement until geometry/content changes.
        val inputs=visuals.map { HubLabelInput(it.node.id,it.anchor,it.occupiedArt?.inflate(3*density.density),
            it.labelSize) }
        val placementPriority=remember(state.hub?.id) { state.selectedNodeId }
        val placements = remember(inputs,image,viewport,reservedBounds) {
            layoutHubLabels(inputs,image,viewport,reservedBounds,placementPriority,4*density.density,minimumTarget=minTarget)
        }
        visuals.forEach { visual ->
            val node=visual.node; val anchor=visual.anchor; val imagePx=visual.imageSize
            val floating=node.id=="astra_access" || node.id=="the_sky"
            val selected=node.id==state.selectedNodeId
            var focused by remember(node.id) { mutableStateOf(false) }
            val interactions=remember(node.id) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val labelHovered by interactions.collectIsHoveredAsState()
            val artInteractions=remember(node.id) { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val artHovered by artInteractions.collectIsHoveredAsState()
            val hovered=labelHovered || artHovered
            val tint = if(settings.highContrastMode) Color.White else when {
                !node.canEnter -> Color(0xFFFFB394)
                selected || state.trackedQuest?.let { nodeMatchesQuest(node,it) }==true -> Color(0xFFFFD477)
                node.completed -> Color(0xFF9BDEC0)
                else -> accent
            }
            val reveal=remember(node.id) { Animatable(if(node.id==state.newlyUnlockedNodeId && motion) 0f else 1f) }
            LaunchedEffect(node.id,state.newlyUnlockedNodeId,motion) {
                if(node.id==state.newlyUnlockedNodeId && motion) { reveal.snapTo(0f); reveal.animateTo(1f,tween(600)) } else reveal.snapTo(1f)
            }
            val placement=placements.getValue(node.id)
            Canvas(Modifier.fillMaxSize()) {
                if(visual.art!=null) {
                    val floatProgress=if(motion && floating) sin(phase+(node.id.hashCode()%7)) else 0f
                    val shadowScale=if(floating) 1f-floatProgress*.08f else 1f
                    val shadowAlpha=(if(node.canEnter) .52f else .28f)*(if(floating) .9f-floatProgress*.1f else 1f)
                    val shadowWidth=imagePx*(if(node.id=="astra_access") .76f else .68f)*shadowScale
                    val shadowHeight=shadowWidth*.28f
                    drawOval(Color.Black.copy(alpha=shadowAlpha),Offset(anchor.x-shadowWidth/2,anchor.y-shadowHeight/2),
                        androidx.compose.ui.geometry.Size(shadowWidth,shadowHeight))
                }
                if(selected || focused || hovered) {
                    val pulse=if(motion) (sin(phase)+1f)/2 else .5f
                    drawOval(tint.copy(alpha=if(selected || focused) .7f else .45f),Offset(anchor.x-28.dp.toPx(),anchor.y-9.dp.toPx()),
                        androidx.compose.ui.geometry.Size(56.dp.toPx(),18.dp.toPx()),style=Stroke(2.dp.toPx()))
                    drawOval(tint.copy(alpha=.18f+pulse*.15f),Offset(anchor.x-(34+pulse*8).dp.toPx(),anchor.y-13.dp.toPx()),
                        androidx.compose.ui.geometry.Size((68+pulse*16).dp.toPx(),26.dp.toPx()),style=Stroke(1.dp.toPx()))
                }
                if(placement.leader) {
                    val edge=Offset(anchor.x.coerceIn(placement.bounds.left,placement.bounds.right),
                        anchor.y.coerceIn(placement.bounds.top,placement.bounds.bottom))
                    drawLine(tint.copy(alpha=if(selected || hovered || focused) .65f else .35f),anchor,edge,1.dp.toPx())
                }
            }
            visual.art?.let { art ->
                Image(visual.painter,null,contentScale=ContentScale.Fit,
                    modifier=Modifier.offset { IntOffset(art.left.roundToInt(),art.top.roundToInt()) }
                        .size(with(density) { imagePx.toDp() }).testTag("hub-art-${node.id}").hoverable(artInteractions).pointerInput(node.id,node.canEnter) {
                            detectTapGestures(onTap={onSelect(node.id)},onDoubleTap={onSelect(node.id);onEnter(node)})
                        }.graphicsLayer {
                            alpha=reveal.value*if(node.canEnter) 1f else .48f
                            translationY=if(motion && floating) sin(phase+(node.id.hashCode()%7))*with(density){3.dp.toPx()} else 0f
                            scaleX=if(selected) 1.04f else 1f;scaleY=scaleX
                        })
            }
            val bounds=placement.bounds
            val targetWidth=bounds.width.coerceAtLeast(minTarget).coerceAtMost(viewport.width)
            val targetHeight=bounds.height.coerceAtLeast(minTarget).coerceAtMost(viewport.height)
            val targetLeft=(bounds.center.x-targetWidth/2).coerceIn(0f,(viewport.width-targetWidth).coerceAtLeast(0f))
            val targetTop=(bounds.center.y-targetHeight/2).coerceIn(0f,(viewport.height-targetHeight).coerceAtLeast(0f))
            TooltipArea(tooltip={
                if(visual.truncated) Surface(color=Color(0xFF061018),shape=RoundedCornerShape(6.dp),border=BorderStroke(1.dp,tint)) {
                    Text(node.title,Modifier.padding(8.dp),color=Color.White,style=MaterialTheme.typography.bodySmall)
                }
            },modifier=Modifier.offset { IntOffset(targetLeft.roundToInt(),targetTop.roundToInt()) }
                .zIndex(3f).size(with(density){targetWidth.toDp()},with(density){targetHeight.toDp()})) {
                Box(Modifier.fillMaxSize().testTag("hub-node-${node.id}").hoverable(interactions)
                    .onFocusChanged { focused=it.hasFocus }.onPreviewKeyEvent {
                        if(it.type==KeyEventType.KeyDown && it.key==Key.Enter) {onSelect(node.id);onEnter(node);true} else false
                    }.combinedClickable(onClick={onSelect(node.id)},onDoubleClick={onSelect(node.id);onEnter(node)})
                    .semantics {
                        contentDescription="${node.title}, ${if(!node.canEnter) "locked" else if(node.completed) "completed" else "available"}"
                        stateDescription=if(selected) "Selected" else if(focused) "Focused" else if(hovered) "Hovered" else "Idle"
                    },
                    contentAlignment=Alignment.Center) {
                    Surface(Modifier.width(with(density){visual.labelSize.width.toDp()}).testTag("hub-nameplate-${node.id}"),
                        shape=RoundedCornerShape(5.dp),color=if(settings.highContrastMode) Color.Black else Color(0xD908121B),
                        border=BorderStroke(if(selected || focused) 1.5.dp else 1.dp,tint.copy(alpha=if(selected || focused || hovered) .85f else .25f))) {
                        Row(Modifier.padding(horizontal=7.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            if(!node.canEnter || node.completed) Icon(if(!node.canEnter) androidx.compose.material.icons.Icons.Default.Lock else androidx.compose.material.icons.Icons.Default.CheckCircle,
                                null,Modifier.size(12.dp),tint=tint)
                            Text(node.title,Modifier.weight(1f),color=if(selected || focused || hovered || settings.highContrastMode) tint else Color(0xFFE0E9EE),
                                style=labelStyle,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
