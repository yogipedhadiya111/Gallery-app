package com.aistudio.xplayer.vdpkzm

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.xplayer.vdpkzm.ui.theme.XPlayerTheme
import com.aistudio.xplayer.vdpkzm.ui.VideoItemRow
import com.aistudio.xplayer.vdpkzm.data.Video
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleVideo = Video(
        uriString = "https://example.com/demo.mp4",
        path = "https://example.com/demo.mp4",
        title = "Sample Cinematic Video Trailer",
        duration = 120000,
        size = 15420000,
        dateAdded = System.currentTimeMillis() / 1000,
        folderName = "Demo Folder"
    )

    composeTestRule.setContent { 
        XPlayerTheme(modeValue = 2) { 
            VideoItemRow(
                video = sampleVideo,
                onVideoClick = {},
                onToggleFav = {},
                onAddToPlaylist = {},
                onRename = {},
                onHide = {},
                onDelete = {},
                onShowInfo = {}
            )
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
