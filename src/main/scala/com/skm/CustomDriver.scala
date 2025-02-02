package com.skm

import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait

final case class CustomDriver(webDriver: WebDriver, waitDriver: WebDriverWait) {
}
