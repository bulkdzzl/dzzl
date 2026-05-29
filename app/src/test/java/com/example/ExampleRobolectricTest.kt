package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.DriverViewModel
import com.example.ui.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("大宗智联-司机端", appName)
  }

  @Test
  fun `test main app loading and rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    
    // Test Splash
    viewModel.showSplashScreen = true
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test login screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = false
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test main tabs rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    
    // Tab 0: Cargo
    viewModel.currentTab = 0
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()

    // Tab 1: Waybills
    viewModel.currentTab = 1
    composeTestRule.waitForIdle()

    // Tab 2: AI Coach
    viewModel.currentTab = 2
    composeTestRule.waitForIdle()

    // Tab 3: Wallet
    viewModel.currentTab = 3
    composeTestRule.waitForIdle()

    // Tab 4: Me
    viewModel.currentTab = 4
    composeTestRule.waitForIdle()
  }

  @Test
  fun `test refueling screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    viewModel.showRefuelingScreen = true
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test bank cards screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    viewModel.showBankCardsScreen = true
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test messages screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    viewModel.showMessageCenterScreen = true
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test reviews screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    viewModel.showReviewsScreen = true
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContainer(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `test settings screen rendering`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DriverViewModel(application)
    viewModel.showSplashScreen = false
    viewModel.isLoggedIn = true
    viewModel.showSettingsScreen = true
    
    composeTestRule.setContent {
      MyApplicationTheme {
        MyApplicationTheme {
          MainAppContainer(viewModel = viewModel)
        }
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }
}
