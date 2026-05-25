package com.example

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.StatCard
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.RentPayTheme
import com.example.ui.theme.ColorSuccess
import com.example.viewmodel.RentPayViewModel
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
    composeTestRule.setContent {
      RentPayTheme {
        StatCard(
          title = "Total Tenants Balance",
          value = "12 Beds Occupied",
          icon = Icons.Default.People,
          gradientColor = ColorSuccess
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun main_app_container_renders() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = RentPayViewModel(app)
    composeTestRule.setContent {
      RentPayTheme {
        MainAppContainer(vm)
      }
    }
    composeTestRule.waitForIdle()
  }
}
