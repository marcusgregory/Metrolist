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

val QueueIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Queue",
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
            // Three horizontal lines (queue/list icon)
            moveTo(3f, 6f)
            horizontalLineTo(21f)
            verticalLineTo(8f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 11f)
            horizontalLineTo(21f)
            verticalLineTo(13f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 16f)
            horizontalLineTo(15f)
            verticalLineTo(18f)
            horizontalLineTo(3f)
            close()
            // Play triangle on bottom right
            moveTo(17f, 14f)
            lineTo(17f, 20f)
            lineTo(22f, 17f)
            close()
        }
    }.build()

val VolumeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Volume",
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
            // Speaker body
            moveTo(3f, 9f)
            verticalLineTo(15f)
            horizontalLineTo(7f)
            lineTo(12f, 20f)
            verticalLineTo(4f)
            lineTo(7f, 9f)
            close()
        }
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
            // Sound waves
            moveTo(15.54f, 8.46f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 12f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15.54f, 15.54f)
            moveTo(18.07f, 5.93f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 12f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18.07f, 18.07f)
        }
    }.build()

val SettingsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Settings",
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
            // Gear outer path (simplified)
            moveTo(12f, 15f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 9f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 15f)
        }
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
            // Gear teeth path
            moveTo(19.4f, 15f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 21f, 14.25f)
            lineTo(21f, 9.75f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19.4f, 9f)
            lineTo(18.65f, 9f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17.93f, 7.34f)
            lineTo(18.35f, 6.64f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17.36f, 4.65f)
            lineTo(13.46f, 2.4f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 11.47f, 2.9f)
            lineTo(10.75f, 3.62f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 3.35f)
            lineTo(9f, 2.6f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7.4f, 1f)
            lineTo(3.6f, 1f)
            arcTo(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2.6f)
        }
    }.build()

val PersonIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Person",
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
            // Head circle
            moveTo(12f, 11f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 11f)
            // Body arc
            moveTo(20f, 21f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 21f)
        }
    }.build()

val HeartIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Heart",
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
            // Heart shape
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            lineTo(12f, 21.35f)
            close()
        }
    }.build()

val PlaylistIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Playlist",
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
            // Music playlist icon (stacked lines with music note)
            moveTo(3f, 10f)
            horizontalLineTo(14f)
            verticalLineTo(12f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 6f)
            horizontalLineTo(14f)
            verticalLineTo(8f)
            horizontalLineTo(3f)
            close()
            moveTo(3f, 14f)
            horizontalLineTo(10f)
            verticalLineTo(16f)
            horizontalLineTo(3f)
            close()
            // Music note
            moveTo(16f, 13f)
            verticalLineTo(17.5f)
            curveTo(16f, 18.88f, 14.88f, 20f, 13.5f, 20f)
            curveTo(12.12f, 20f, 11f, 18.88f, 11f, 17.5f)
            curveTo(11f, 16.12f, 12.12f, 15f, 13.5f, 15f)
            curveTo(13.84f, 15f, 14.16f, 15.07f, 14.45f, 15.19f)
            lineTo(14.45f, 10f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(16f)
            close()
        }
    }.build()
