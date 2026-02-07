package com.metrolist.music.wear.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PlayIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(8f, 5f)
            lineTo(8f, 19f)
            lineTo(19f, 12f)
            close()
        }
    }.build()

val PauseIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6f, 19f)
            horizontalLineTo(10f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            close()
            moveTo(14f, 5f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            horizontalLineTo(14f)
            close()
        }
    }.build()

val SkipNextIcon: ImageVector
    get() = ImageVector.Builder(
        name = "SkipNext",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6f, 18f)
            lineTo(14.5f, 12f)
            lineTo(6f, 6f)
            close()
            moveTo(16f, 6f)
            verticalLineTo(18f)
            horizontalLineTo(18f)
            verticalLineTo(6f)
            horizontalLineTo(16f)
            close()
        }
    }.build()

val SkipPreviousIcon: ImageVector
    get() = ImageVector.Builder(
        name = "SkipPrevious",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6f, 6f)
            horizontalLineTo(8f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
            moveTo(9.5f, 12f)
            lineTo(18f, 18f)
            verticalLineTo(6f)
            close()
        }
    }.build()

val SearchIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1f,
            stroke = SolidColor(Color.Black),
            strokeAlpha = 1f,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            // Circle
            moveTo(11f, 19f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 3f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 19f)
            // Line
            moveTo(21f, 21f)
            lineTo(16.65f, 16.65f)
        }
    }.build()
