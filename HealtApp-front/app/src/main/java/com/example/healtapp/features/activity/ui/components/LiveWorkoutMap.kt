package com.example.healtapp.features.activity.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.healtapp.BuildConfig
import com.example.healtapp.features.activity.live.GpsPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val cartoVoyager: OnlineTileSourceBase = object : XYTileSource(
    "CartoVoyager",
    1,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "© OpenStreetMap © CARTO",
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
    }
}

@Composable
fun LiveWorkoutMap(
    points: List<GpsPoint>,
    headingDegrees: Float?,
    previewLat: Double? = null,
    previewLon: Double? = null,
    followUser: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val arrowBitmap = remember { createHeadingArrowBitmap() }
    val mapView = remember {
        ensureOsmConfig(context)
        MapView(context).apply {
            setTileSource(cartoVoyager)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            isTilesScaledToDpi = true
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp)),
        update = { map ->
            val track = points.map { GeoPoint(it.lat, it.lon) }
            val last = track.lastOrNull()
                ?: previewLat?.let { lat ->
                    previewLon?.let { lon -> GeoPoint(lat, lon) }
                }
            map.overlays.removeAll { it is Polyline || it is Marker }

            if (track.size >= 2) {
                map.overlays.add(
                    Polyline().apply {
                        setPoints(track)
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.color = 0xFF1FA97A.toInt()
                    },
                )
            }

            if (last != null) {
                map.overlays.add(
                    Marker(map).apply {
                        position = last
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = android.graphics.drawable.BitmapDrawable(context.resources, arrowBitmap)
                        setFlat(true)
                        rotation = headingDegrees ?: 0f
                        title = "Вы"
                    },
                )
                if (followUser) {
                    map.controller.setCenter(last)
                }
            }
            map.invalidate()
        },
    )
}

private fun ensureOsmConfig(context: Context) {
    val cfg = Configuration.getInstance()
    cfg.userAgentValue = "HealthApp/${BuildConfig.VERSION_NAME} (${context.packageName})"
    cfg.osmdroidBasePath = context.cacheDir.resolve("osmdroid")
    cfg.osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
}

/** Стрелка направления движения — чистая геометрия, без дефолтного маркера OSM. */
private fun createHeadingArrowBitmap(): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1FA97A.toInt()
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val path = Path().apply {
        moveTo(size / 2f, 10f)
        lineTo(size - 14f, size - 16f)
        lineTo(size / 2f, size - 30f)
        lineTo(14f, size - 16f)
        close()
    }
    canvas.drawPath(path, fill)
    canvas.drawPath(path, stroke)
    return bitmap
}
