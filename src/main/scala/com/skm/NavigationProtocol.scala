package com.skm

import com.skm.NavigationProtocol.Element._
import com.skm.NavigationProtocol.WaitCondition.WaitFlag
import com.skm.NavigationProtocol._
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.{ ExpectedCondition, ExpectedConditions }
import org.openqa.selenium.{
  By,
  Dimension,
  JavascriptExecutor,
  Keys,
  OutputType,
  Point,
  Rectangle,
  TakesScreenshot,
  WebDriver,
  WebElement
}

import java.nio.file.{ Files, Path, Paths, StandardCopyOption }
import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{ Failure, Success, Try }

/**
 * A lightweight library built to simplify browser automation with Selenium. It provides a dialogue-style interface,
 * allowing you to define navigation steps in a concise, readable, and declarative manner.
 * @param driver selenium driver
 */
final class NavigationProtocol private (
  driver: CustomDriver,
  private val currentState: Try[Element],
  private val screenshots: List[Screenshot]
) { self =>

  /**
   * attempts to locate a web element in the browser using the specified selector (e.g., CSS or XPath) and an
   * optional visibility filter. If the element is found, it updates the NavigationProtocol's current state to reflect
   * the fetched element. If no element is found, or the visibility filter excludes all matches, the current state is
   * updated to an error state with a descriptive failure reason.
 *
   * @param selector the CSS selector that identifies the element to fetch
   * @param filter Specifies whether to fetch any matching element (VisibilityFilter.Any) or only those that are
   *               currently visible (VisibilityFilter.Visible).
   * @return If the element is successfully found (and meets the visibility criteria), the state is updated to hold the
   *         located element.
   *         If no matching element is found or the visibility filter excludes all matches, the state is updated with a
   *         Failure.
   */
  def fetch(selector: By, filter: VisibilityFilter = VisibilityFilter.Visible): NavigationProtocol =
    updateState {
      findElement(driver, selector).flatMap { elem =>
        filter match {
          case VisibilityFilter.Any => Success(Element(elem, selector.toString))
          case VisibilityFilter.Visible =>
            if (elem.isDisplayed) Success(Element(elem, selector.toString))
            else Failure(new NoSuchElementException(s"No visible element found matching $selector"))
        }
      }
    }

  /**
   * an alias for @fetch
   * @see fetch
   */
  def thenFetch(selector: By, filter: VisibilityFilter = VisibilityFilter.Visible): NavigationProtocol =
    fetch(selector, filter)

  /**
   * scrolls the browser view to the currently selected element within the navigation context.
   * This ensures that the element becomes visible in the viewport. The method leverages Selenium's Actions class to
   * perform the scrolling action, targeting the element represented by the current state of the NavigationProtocol.
   * If the current state does not contain a valid element, the operation is skipped, and the state remains unchanged.
   */
  def scrollToElement: NavigationProtocol =
    updateState {
      for {
        state   <- currentState
        element = state.webElement
        _       <- Try(new Actions(driver.webDriver).scrollToElement(element).perform())
      } yield state
    }

  /**
   * a convenience alias for the scrollToElement function. It provides a more fluent, dialogue-like syntax,
   * allowing you to chain the scrolling operation seamlessly as part of a navigation sequence.
   * This method ensures that the currently selected element is scrolled into view, maintaining the state of the
   * NavigationProtocol and enabling further actions to be performed. If no valid element exists in the current state,
   * the operation is skipped without modifying the state.
   */
  def thenScrollToElement: NavigationProtocol = scrollToElement

  /**
   * performs a click action on the currently selected element in the NavigationProtocol's state.
   * If the current state contains a valid element, it simulates a mouse click on that element.
   * The function ensures that the state remains unchanged upon successful execution, allowing further operations
   * to be chained. If no valid element is present in the current state, the operation is skipped, and the existing
   * state is retained.
   * Any errors encountered during the click action are captured and propagated through the updated state.
   */
  def click(): NavigationProtocol =
    updateState {
      for {
        state <- currentState
        _     <- Try(state.webElement.click())
      } yield state
    }

  /**
   * an alias for the click function, providing a more natural way to chain the click operation in a sequence of
   * navigation steps. It performs a mouse click on the currently selected element if a valid element is present in
   * the current state of the NavigationProtocol. The method updates the protocol's state based on the success or
   * failure of the operation, enabling seamless integration with other chained actions.
   * This alias enhances readability in scenarios where a fluent, dialogue-like navigation style is preferred.
   * @see click()
   */
  def thenClick(): NavigationProtocol = click()

  /**
   * pauses the execution of the NavigationProtocol until a specified condition is met. It evaluates the given condition
   * and, if successful, allows the navigation to proceed. If the condition fails, the state of the NavigationProtocol
   * is updated to reflect the failure.
   * This function is particularly useful for ensuring that the browser has reached a stable state before interacting
   * with elements, such as waiting for an animation to complete or an element to appear on the page.
   * It integrates seamlessly into the navigation flow, enabling robust and reliable automation scripts.
   * @param condition a custom condition that defines the criteria to wait for. This could involve checking for the
   *                  presence of an element, waiting for an element to become visible, or any other user-defined
   *                  condition.
   *
   */
  def thenWaitUntil(condition: WaitCondition): NavigationProtocol =
    updateState(condition.evaluate(driver).flatMap(_ => currentState))

  /**
   * Validates that the text content of the currently selected element matches a specified value. If the assertion
   * succeeds, the NavigationProtocol continues with the current state. Otherwise, the state is updated to an error
   * indicating the assertion failure.
   * <p>
   * <code>NavigationProtocol(driver).fetch(By.id("confirmationMessage")).thenAssertText("Booking Confirmed")</code>
   * @param value The expected text value to compare against the text content of the selected element.
   *              The comparison is case-insensitive.
   */
  def thenAssertText(value: String): NavigationProtocol =
    updateState {
      currentState match {
        case Failure(_) => currentState
        case Success(element) =>
          Try(element.webElement.getText) match {
            case Failure(e) => Failure(new Exception(s"error reading text from element ${element.identifier}", e))
            case Success(text) =>
              if (Option(text).exists(_.equalsIgnoreCase(value))) currentState
              else
                Failure(new Exception(s"assertion failure: expected '$value' found '$text'"))
          }
      }
    }

  /**
   * checks whether the currently selected element is enabled (i.e., interactable).
   * This method is part of a chained navigation flow and is used to validate that an element can be interacted with
   * before performing further actions.
   * <p>
   * If the element exists and is enabled, the NavigationProtocol retains its current state, allowing subsequent
   * operations to proceed without interruption. If the element is disabled or not interactable, the protocol's state
   * is updated to an error. An exception with a descriptive message, including the element's identifier,
   * is added to the state. This function ensures that actions like clicking or sending input are only performed on
   * enabled elements, thereby reducing the risk of runtime errors and improving the robustness of navigation flows.
   * <p>
   * <code>NavigationProtocol(driver)
   * .fetch(By.id("submitButton"))
   * .thenAssertEnabled
   * .thenClick()</code>
   */
  def thenAssertEnabled: NavigationProtocol =
    updateState {
      currentState.flatMap(
        element =>
          if (element.webElement.isEnabled) currentState
          else Failure(new Exception(s"Element ${element.identifier} is not enabled"))
      )
    }

  /**
   * sends a sequence of keystrokes to the currently selected element in the navigation flow. This method allows text
   * input or other key-based interactions with elements, such as text fields, dropdowns,
   * or any element that can accept input.
   * If the current state contains a valid, enabled element, the specified keystrokes are sent to the element.
   * The NavigationProtocol retains its current state, allowing subsequent operations.
   * If the element is invalid, missing, or cannot accept input, the protocol's state transitions to a failure,
   * halting further navigation and logging an error.
   * This function simplifies the process of interacting with input fields or other interactive elements, ensuring that
   * keystrokes are sent in a structured and controlled manner.
   * <p>
   * <code>val protocol = NavigationProtocol(driver)
   * .fetch(By.id("usernameField"))
   * .thenAssertEnabled
   * .thenSendKeys("test_user")</code>
 *
   * @param keys The sequence of characters to send to the element.
   */
  def thenSendKeys(keys: String): NavigationProtocol =
    updateState {
      currentState.flatMap(element => Try(element.webElement.sendKeys(keys)).flatMap(_ => currentState))
    }

  /**
   * Attempts to click on the current element if it contains a specific attribute with a specified value.
   * Checks whether the current element has an attribute matching the provided attributeName and whether
   * its value equals (case-insensitively) the provided attributeValue.
   * If the condition is met, the element is clicked.
   * If the condition is not met or an error occurs, the navigation state remains unchanged.
   * This function is useful for conditional interactions with elements, ensuring that a click is performed only
   * when specific attribute conditions are satisfied
   * @param attributeName the name of the attribute to evaluate.
   * @param attributeValue the expected value of the attribute.
   */
  def thenClickIf(attributeName: String, attributeValue: String): NavigationProtocol =
    updateState {
      currentState.flatMap { element =>
        (for {
          attr <- Try(element.webElement.getAttribute(attributeName))
          flg  <- Try(Option(attr).map(_.equalsIgnoreCase(attributeValue)))
        } yield flg) match {
          case Failure(e) => Failure(e)
          case Success(results) =>
            if (results.contains(true)) Try(click()).flatMap(_ => currentState)
            else currentState
        }
      }
    }

  /**
   * Executes additional navigation actions or integrates results into the current navigation protocol.
   * If results is a success, it updates the currentState with the new currentElement and appends the screenShots
   * to the existing list.
   * If results is a failure, the protocol transitions to a failed state, retaining the error.
   * @param results The results of a navigation action to be applied to the current protocol.
   */
  def thenDo(results: Try[NavigationResults]): NavigationProtocol =
    results match {
      case Failure(e)   => NavigationProtocol(driver, Failure(e), List.empty)
      case Success(res) => NavigationProtocol(driver, Success(res.currentElement), screenshots ++ res.screenShots)
    }

  /**
   * Executes additional navigation actions or integrates results into the current navigation protocol.
   * If the current protocol is in a success state, the currentState of the provided navigation is merged into
   * the current instance. All screenshots from the navigation are appended to the existing list.
   * If the current protocol is in a failure state, no changes are made.
   * @param navigation Another NavigationProtocol instance to execute and merge into the current protocol.
   */
  def thenDo(navigation: NavigationProtocol): NavigationProtocol =
    evaluate match {
      case Failure(_)     => self
      case Success(value) => NavigationProtocol(driver, navigation.currentState, self.screenshots ++ value.screenShots)
    }

  /**
   * Highlights the current element in the browser for visual debugging by briefly changing its style.
   * Attempts to apply a temporary highlight effect to the element currently in focus (currentState).
   * <p>
   * The highlighting process involves:
   * <ul>
   *   <li>Storing the element's original style.cssText using JavaScript execution.</li>
   *   <li>Adding a yellow background to the element's style.</li>
   *   <li>Pausing for 2 seconds to allow the highlight to be noticeable.</li>
   *   <li>Restoring the original style.</li>
   * </ul>
   * If the currentState is in a failure state, the method does nothing and retains the failure state.
   */
  def thenHighlightElement: NavigationProtocol =
    updateState {
      for {
        jsExecutor    <- Try(driver.webDriver.asInstanceOf[JavascriptExecutor])
        state         <- currentState
        element       = state.webElement
        originalStyle <- Try(jsExecutor.executeScript("return arguments[0].style.cssText;", element))
        _             <- Try(jsExecutor.executeScript("arguments[0].style.cssText += 'background: yellow;';", element))
        _             <- Try(Thread.sleep(2000))
        _             <- Try(jsExecutor.executeScript("arguments[0].style.cssText = arguments[1];", element, originalStyle))
      } yield state
    }

  /**
   * Attempts to capture a screenshot of the browser's current viewport using Selenium's TakesScreenshot interface.
   * The screenshot is saved as a file with the format [name].[extension] in the specified targetFolder.
   * <p>
   * If the operation is successful:
   * <ul>
   *   <li>The screenshot is stored as an instance of Screenshot, containing the name and file path.</li>
   *   <li>The new Screenshot is appended to the list of screenshots in the protocol.</li>
   *   <li>If currentState is in a failure state, the method does nothing and retains the failure state.</li>
   * </ul>
   * @param name A descriptive name for the screenshot file. This name will be used as the base for the file's name.
   * @param targetFolder Path (default: Paths.get("target", "log"))
   *                     The folder where the screenshot file will be saved.
   */
  def takeScreenshot(name: String, targetFolder: Path = Paths.get("target", "log")): NavigationProtocol =
    currentState match {
      case Failure(_) => self
      case Success(_) =>
        (for {
          tempFile <- Try(
                       driver.webDriver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
                     )
          fileName <- FileName.extractFileName(tempFile.getName)
          path <- Try(
                   Files.copy(
                     tempFile.toPath,
                     targetFolder.resolve(s"$name.${fileName.extension}"),
                     StandardCopyOption.REPLACE_EXISTING
                   )
                 )
          screenshot <- Screenshot(name, path)
        } yield screenshot) match {
          case Failure(e)  => NavigationProtocol(driver, Failure(e))
          case Success(ss) => NavigationProtocol(driver, currentState, screenshots :+ ss)
        }
    }

  /**
   * An alias for the function takeScreenshot. Designed for fluent chaining with other navigation steps.
   * Allows combining multiple actions seamlessly in a single statement, enhancing readability.
   * @see takeScreenshot
   */
  def thenTakeScreenshot(name: String, targetFolder: Path = Paths.get("target", "log")): NavigationProtocol =
    takeScreenshot(name, targetFolder)

  /**
   * Scrolls through a container element, capturing screenshots at each step, until the end of the container is reached.
   * Identifies the container element using the provided selector then uses a recursive function (loop) to:
   * <ul>
   *   <li>Scroll the container by a page down or arrow down key using Selenium's Actions class.</li>
   *   <li>Capture a screenshot at each step.</li>
   *   <li>Wait briefly after each scroll to allow the content to stabilize.</li>
   *   <li>Compare the current scroll position (scrollTop) to the previous position to determine if scrolling has
   *   reached the bottom.</li>
   * </ul>
   * @param selector The locator used to identify the container element to scroll.
   * @param name The base name for each screenshot file.
   *             Screenshots will be named incrementally (e.g., name-1.png, name-2.png).
   */
  def thenScrollToEndWithScreenshots(selector: By, name: String): NavigationProtocol = {
    @annotation.tailrec
    def loop(
      lastScrollTop: Int,
      screenshots: List[Screenshot],
      actions: Actions,
      container: WebElement,
      shotCounter: Int
    ): NavigationProtocol = {
      val result =
        for {
          _          <- Try(actions.moveToElement(container).sendKeys(container, Keys.PAGE_DOWN).perform())
          screenshot <- takeScreenshot(s"$name-$shotCounter").evaluate.map(_.screenShots)
          _          <- Try(Thread.sleep(1000))
          currentScrollTop <- Try(
                               driver.webDriver
                                 .asInstanceOf[JavascriptExecutor]
                                 .executeScript("return arguments[0].scrollTop;", container)
                                 .asInstanceOf[Long]
                                 .toInt
                             )
          _ <- if (lastScrollTop == currentScrollTop)
                Try(actions.moveToElement(container).sendKeys(container, Keys.ARROW_DOWN).perform())
              else
                Success(())
        } yield (screenshot, currentScrollTop)

      result match {
        case Failure(e) => NavigationProtocol(driver, Failure(e))
        case Success((screenshot, currentScrollTop)) =>
          if (lastScrollTop == currentScrollTop) {
            (for {
              _ <- Try(actions.moveToElement(container).sendKeys(container, Keys.PAGE_DOWN).perform())
              lastShot <- NavigationProtocol(driver, currentState, List.empty)
                           .takeScreenshot(s"$name-$shotCounter")
                           .evaluate
                           .map(_.screenShots)
            } yield lastShot) match {
              case Failure(e)        => NavigationProtocol(driver, Failure(e))
              case Success(lastShot) => NavigationProtocol(driver, currentState, screenshot ++ screenshots ++ lastShot)
            }
          } else {
            loop(currentScrollTop, screenshot ++ screenshots, actions, container, shotCounter + 1)
          }
      }
    }

    (for {
      actions   <- Try(new Actions(driver.webDriver))
      container <- findElement(driver, selector)
      result    = loop(-1, List.empty, actions, container, 1)
    } yield result) match {
      case Failure(e)        => NavigationProtocol(driver, Failure(e))
      case Success(protocol) => protocol
    }
  }

  /**
   * Executes the current navigation state and produces the final result, capturing any outputs or errors
   * that occur during the process.
   * This function acts as the final step in the navigation process, ensuring all defined actions are executed
   * and their results are captured in a structured manner.
   * <p>
   * State Execution:
   * Triggers the execution of the current NavigationProtocol, applying all accumulated actions
   * <code>(e.g., element interactions, assertions, scrolling)</code>.
   * <p>
   * Result Evaluation:
   * If the execution is successful, it returns the desired output, such as captured screenshots,
   * navigation results, or other side effects.
   * If an error occurs during execution, it captures the failure and propagates it as part of the returned state.
   * <p>
   * Error Handling:
   * Safeguards against runtime errors during interactions with the browser or elements by wrapping all operations in Try.
   *
   */
  def evaluate: Try[NavigationResults] =
    currentState match {
      case Failure(exception) => Failure(exception)
      case Success(state)     => Success(NavigationResults(state, screenshots))
    }

  /**
   * Updates the current state of the NavigationProtocol by appending new actions or results to the existing
   * chain of operations.
   * <p>
   * State Transformation:
   * Modifies the current state of the protocol by incorporating additional navigation steps or side effects
   * (e.g., results from an executed action).
   * Ensures immutability by returning a new NavigationProtocol instance with the updated state.
   * <p>
   * Action Composition:
   * Supports chaining by adding new actions (e.g., element interactions, assertions) to the protocol.
   * Keeps track of intermediate results or any new information required for subsequent steps.
   * <p>
   * Error Preservation:
   * If the protocol is already in a failure state, the existing error is preserved, and no further updates are applied.
   *
   * @param newState Represents the new state or actions to be appended to the protocol.
   */
  private def updateState(newState: => Try[Element]): NavigationProtocol =
    currentState match {
      case Failure(_) => self
      case Success(_) => NavigationProtocol(driver, newState, screenshots)
    }
}

object NavigationProtocol {

  def apply(driver: CustomDriver): NavigationProtocol = new NavigationProtocol(driver, Success(placeHolder), List.empty)

  def apply(
    driver: CustomDriver,
    currentState: Try[Element],
    screenshots: List[Screenshot] = List.empty
  ): NavigationProtocol =
    new NavigationProtocol(driver, currentState, screenshots)

  sealed trait VisibilityFilter

  object VisibilityFilter {
    final object Any     extends VisibilityFilter
    final object Visible extends VisibilityFilter
  }

  private def findElement(driver: CustomDriver, elementId: By): Try[WebElement] =
    for {
      _       <- Try(driver.waitDriver.until(ExpectedConditions.presenceOfElementLocated(elementId)))
      element <- Try(driver.webDriver.findElement(elementId))
    } yield element

  private def findElements(driver: CustomDriver, selector: By): Try[List[WebElement]] =
    for {
      _        <- Try(driver.waitDriver.until(ExpectedConditions.presenceOfAllElementsLocatedBy(selector)))
      elements <- Try(driver.webDriver.findElements(selector))
    } yield elements.asScala.toList

  final case class Element(webElement: WebElement, identifier: String) {
    def isPlaceHolder: Boolean = placeholderIdentifier.equalsIgnoreCase(identifier)
  }

  object Element {
    val placeholderIdentifier = "[placeholder element]"
  }

  final case class WaitCondition(selector: By, flag: WaitFlag) {

    def evaluate(driver: CustomDriver): Try[Boolean] = flag match {
      case WaitFlag.IsPresent =>
        evaluateCondition(driver, !_.isEmpty, s"Waiting for element identified by $selector to appear.")
      case WaitFlag.IsAbsent =>
        evaluateCondition(driver, _.isEmpty, s"Waiting for element identified by $selector to stop appearing.")
    }

    private def evaluateCondition(
      driver: CustomDriver,
      f: util.List[WebElement] => Boolean,
      message: String
    ): Try[Boolean] =
      Try {
        driver.waitDriver
          .until(new ExpectedCondition[Boolean] {
            override def apply(d: WebDriver): Boolean = {
              val elem: util.List[WebElement] = driver.webDriver.findElements(selector)
              f(elem)
            }
            override def toString: String = message
          })
      }
  }

  object WaitCondition {
    sealed trait WaitFlag

    object WaitFlag {
      final object IsPresent extends WaitFlag
      final object IsAbsent  extends WaitFlag
    }
  }

  final case class NavigationResults(currentElement: Element, screenShots: List[Screenshot]) {
    def dropScreenshot(name: String): List[Screenshot] = screenShots.filterNot(_.name == name)
  }

  private def err: Nothing =
    throw new IllegalStateException("Invalid state. You need to first fetch an element.")

  val placeHolder: Element = Element(
    new WebElement {
      override def click(): Unit                                = err
      override def submit(): Unit                               = err
      override def sendKeys(keysToSend: CharSequence*): Unit    = err
      override def clear(): Unit                                = err
      override def getTagName: String                           = err
      override def getAttribute(name: String): String           = err
      override def isSelected: Boolean                          = err
      override def isEnabled: Boolean                           = err
      override def getText: String                              = err
      override def findElements(by: By): util.List[WebElement]  = err
      override def findElement(by: By): WebElement              = err
      override def isDisplayed: Boolean                         = err
      override def getLocation: Point                           = err
      override def getSize: Dimension                           = err
      override def getRect: Rectangle                           = err
      override def getCssValue(propertyName: String): String    = err
      override def getScreenshotAs[X](target: OutputType[X]): X = err
    },
    Element.placeholderIdentifier
  )
}
