package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

class Int16GeoTiffTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  val cellType: ShortCells with NoDataHandling
) extends GeoTiffTile(segmentLayout, compression) with Int16GeoTiffSegmentCollection {

  val noDataValue: Option[Short] = cellType match {
    case ShortCellType => None
    case ShortConstantNoDataCellType => Some(Short.MinValue)
    case ShortUserDefinedNoDataCellType(nd) => Some(nd)
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Short](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment =
        getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          val data = segment.get(i)
          arr(row * cols + col) = data
        }
      }
    }

    ShortArrayTile(arr, cols, rows, cellType)
  }

  def mutable(windowedGeoTiff: WindowedGeoTiff): MutableArrayTile = {
    val windowedGridBounds = windowedGeoTiff.windowedGridBounds
    val intersectingSegments = windowedGeoTiff.intersectingSegments
    val arr = Array.ofDim[Short](windowedGridBounds.size)

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
    ShortArrayTile(arr, windowedGridBounds.width, windowedGridBounds.height, cellType)
  }
}
