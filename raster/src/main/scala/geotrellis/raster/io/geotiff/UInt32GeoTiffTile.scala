package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class UInt32GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: FloatCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with UInt32GeoTiffSegmentCollection {

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Float](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment =
        getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.get(i)
        }
      }
    }
    FloatArrayTile(arr, cols, rows, cellType)
  }

  def mutable(windowedGeoTiff: WindowedGeoTiff): MutableArrayTile = {
    val windowedGridBounds = windowedGeoTiff.windowedGridBounds
    val intersectingSegments = windowedGeoTiff.intersectingSegments
    val arr = Array.ofDim[Float](windowedGridBounds.size)
    
    val colMin = windowedGridBounds.colMin
    val rowMin = windowedGridBounds.rowMin
    val width = windowedGridBounds.width

    for (segmentIndex <- intersectingSegments) {
      val segment = getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)

      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if (windowedGridBounds.contains(col, row))
          arr((row - rowMin) * width + (col - colMin)) = segment.get(i)
      }
    }
    FloatArrayTile(arr, windowedGridBounds.width, windowedGridBounds.height, cellType)
  }
}
