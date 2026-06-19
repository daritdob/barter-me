package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BarterViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.SOFTWARE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [28])
@Ignore("Compose screenshot tests run locally; CI uses AuthRepositoryTest for auth coverage")
class AuthScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun auth_screen_renders() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)

    composeTestRule.setContent {
      val viewModel: BarterViewModel = viewModel(factory = viewModelFactory)
      MyApplicationTheme(darkTheme = true) {
        AuthScreen(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Ignore("Roborazzi capture is run locally; headless CI uses AuthRepositoryTest instead")
  @Test
  fun auth_screen_screenshot() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)

    composeTestRule.setContent {
      val viewModel: BarterViewModel = viewModel(factory = viewModelFactory)
      MyApplicationTheme(darkTheme = true) {
        AuthScreen(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/auth_screen.png")
  }
}
