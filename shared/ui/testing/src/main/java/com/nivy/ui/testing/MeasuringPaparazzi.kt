package com.nivy.ui.testing

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import java.awt.image.BufferedImage

/**
 * Paparazzi drawing a Pixel 6 Pro's frame at its full resolution, without the system bars, and
 * handing each frame to [onFrame] for the test to measure, rather than writing or verifying it.
 */
fun measuringPaparazzi(onFrame: (BufferedImage) -> Unit) = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_6_PRO,
    showSystemUi = false,
    useDeviceResolution = true,
    snapshotHandler = object : SnapshotHandler {
        override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) =
            object : SnapshotHandler.FrameHandler {
                override fun handle(image: BufferedImage) = onFrame(image)

                override fun close() = Unit
            }

        override fun close() = Unit
    },
)
