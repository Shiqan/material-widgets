package com.example.materialwidgets.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.materialwidgets.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MainScreen(FAKE_DATA) }
  }

  @Test
  fun widgetGalleryCards_exist() {
    composeTestRule.onNodeWithText("Material You Dynamic Color").assertExists()
    composeTestRule.onNodeWithText("Material You Clock").assertExists()
    composeTestRule.onNodeWithText("Material You Weather").assertExists()
    composeTestRule.onNodeWithText("Material You Music").assertExists()
  }
}

private val FAKE_DATA = listOf("Sample1", "Sample2", "Sample3")
