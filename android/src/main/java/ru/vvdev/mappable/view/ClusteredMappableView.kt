package ru.vvdev.mappable.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import ru.vvdev.mappable.models.ReactMapObject
import world.mappable.mapkit.geometry.Point
import world.mappable.mapkit.map.Cluster
import world.mappable.mapkit.map.ClusterListener
import world.mappable.mapkit.map.ClusterTapListener
import world.mappable.mapkit.map.IconStyle
import world.mappable.mapkit.map.PlacemarkMapObject
import world.mappable.runtime.image.ImageProvider
import kotlin.math.abs
import kotlin.math.sqrt

class ClusteredMappableView(context: Context?) : MappableView(context), ClusterListener,
    ClusterTapListener {
    private val clusterCollection = mapWindow.map.mapObjects.addClusterizedPlacemarkCollection(this)
    private var clusterColor = 0
    private val placemarksMap: HashMap<String?, PlacemarkMapObject?> = HashMap<String?, PlacemarkMapObject?>()
    private var pointsList = ArrayList<Point>()

    fun setClusteredMarkers(points: ArrayList<Any?>) {
        clusterCollection.clear()
        placemarksMap.clear()
        val pt = ArrayList<Point>()
        for (i in points.indices) {
            val point = points[i] as HashMap<String, Double>
            pt.add(Point(point["lat"]!!, point["lon"]!!))
        }
        val placemarks = clusterCollection.addPlacemarks(pt, TextImageProvider(""), IconStyle())
        pointsList = pt
        for (i in placemarks.indices) {
            val placemark = placemarks[i]
            placemarksMap["" + placemark.geometry.latitude + placemark.geometry.longitude] =
                placemark
            val child: Any? = getChildAt(i)
            if (child != null && (child is IMarker)) {
                child.setMarkerMapObject(placemark)
            }
        }
        clusterCollection.clusterPlacemarks(50.0, 12)
    }

    fun setClustersColor(color: Int) {
        clusterColor = color
        updateUserMarkersColor()
    }

    private fun updateUserMarkersColor() {
        clusterCollection.clear()
        val placemarks = clusterCollection.addPlacemarks(
            pointsList,
            TextImageProvider(pointsList.size.toString()),
            IconStyle()
        )
        for (i in placemarks.indices) {
            val placemark = placemarks[i]
            placemarksMap["" + placemark.geometry.latitude + placemark.geometry.longitude] =
                placemark
            val child: Any? = getChildAt(i)
            if (child != null && child is IMarker) {
                child.setMarkerMapObject(placemark)
            }
        }
        clusterCollection.clusterPlacemarks(50.0, 12)
    }

    override fun addFeature(child: View?, index: Int) {
        if (child is IMarker) {
            val marker = child as IMarker
            val placemark = placemarksMap["" + marker.getPoint()!!.latitude + marker.getPoint()!!.longitude]
            if (placemark != null) {
                child.setMarkerMapObject(placemark)
            }
        } else if (child is MappablePolygon) {
            val _child = child
            val obj = mapWindow.map.mapObjects.addPolygon(_child.polygon)
            _child.setPolygonMapObject(obj)
        } else if (child is MappablePolyline) {
            val _child = child
            val obj = mapWindow.map.mapObjects.addPolyline(_child.polyline)
            _child.setPolylineMapObject(obj)
        } else if (child is MappableCircle) {
            val _child = child
            val obj = mapWindow.map.mapObjects.addCircle(_child.circle)
            _child.setCircleMapObject(obj)
        }
    }

    override fun removeChild(index: Int) {
        if (getChildAt(index) is IMarker) {
            val child = getChildAt(index) as IMarker ?: return
            val mapObject = (child as ReactMapObject).rnMapObject
            if (mapObject == null || !mapObject.isValid) return
            clusterCollection.remove(mapObject)
            placemarksMap.remove("" + child.getPoint()!!.latitude + child.getPoint()!!.longitude)
        } else {
           super.removeChild(index)
        }
    }

    override fun onClusterAdded(cluster: Cluster) {
        cluster.appearance.setIcon(TextImageProvider(cluster.size.toString()))
        cluster.addClusterTapListener(this)
    }

    override fun onClusterTap(cluster: Cluster): Boolean {
        val points = ArrayList<Point?>()
        for (placemark in cluster.placemarks) {
            points.add(placemark.geometry)
        }
        fitMarkers(points)
        return true
    }

    private inner class TextImageProvider(private val text: String) : ImageProvider() {
        override fun getId(): String {
            return "text_$text"
        }

        override fun getImage(): Bitmap {
            val textPaint = Paint()
            textPaint.textSize = Companion.FONT_SIZE
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.style = Paint.Style.FILL
            textPaint.isAntiAlias = true

            val widthF = textPaint.measureText(text)
            val textMetrics = textPaint.fontMetrics
            val heightF =
                (abs(textMetrics.bottom.toDouble()) + abs(textMetrics.top.toDouble())).toFloat()
            val textRadius = sqrt((widthF * widthF + heightF * heightF).toDouble())
                .toFloat() / 2
            val internalRadius = textRadius + Companion.MARGIN_SIZE
            val externalRadius = internalRadius + Companion.STROKE_SIZE

            val width = (2 * externalRadius + 0.5).toInt()

            val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val backgroundPaint = Paint()
            backgroundPaint.isAntiAlias = true
            backgroundPaint.color = clusterColor
            canvas.drawCircle(
                (width / 2).toFloat(),
                (width / 2).toFloat(),
                externalRadius,
                backgroundPaint
            )

            backgroundPaint.color = Color.WHITE
            canvas.drawCircle(
                (width / 2).toFloat(),
                (width / 2).toFloat(),
                internalRadius,
                backgroundPaint
            )

            canvas.drawText(
                text,
                (width / 2).toFloat(),
                width / 2 - (textMetrics.ascent + textMetrics.descent) / 2,
                textPaint
            )

            return bitmap
        }
    }
    companion object {
        private const val FONT_SIZE = 45f
        private const val MARGIN_SIZE = 9f
        private const val STROKE_SIZE = 9f
    }
}
