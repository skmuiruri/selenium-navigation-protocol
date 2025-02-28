
# Selenium-Navigation-Protocol

`NavigationProtocol` provides a structured, dialogue-like approach to browser navigation and interaction, encapsulating the state of navigation, browser elements, and screenshots in a cohesive and immutable way. It is designed to simplify complex web automation workflows by offering a fluent API for interacting with web elements, performing validations, and enhancing visibility into browser actions.

## Features:

* **State Management**:
Maintains the current navigation state as a Try[Element], allowing seamless handling of success or failure scenarios at any stage of the navigation process.

* **Element Interaction**:
Offers intuitive methods such as fetch, click, scrollToElement, and sendKeys, enabling direct interaction with elements identified by CSS or XPath selectors. Conditional actions like clickIf allow greater control over dynamic behaviors.

* **Validation and Assertions**:
Provides built-in support for asserting element properties, such as text content or enabled state, with methods like thenAssertText and thenAssertEnabled, ensuring robust validation during test execution.

* **Visual Debugging**:
Includes tools for highlighting elements on the page and capturing screenshots, enhancing debugging and reporting capabilities. Screenshots can be taken during any navigation step, and scrolling workflows with screenshots are supported for better visibility of dynamic content.

* **Fluent and Composable API**:
Methods like thenDo, thenFetch, and thenScrollToElement promote a fluent and chainable API design, allowing developers to compose complex navigation flows in a readable and maintainable manner.

* **Robust Error Handling**:
Automatically propagates errors encountered during navigation or interactions, preserving the integrity of the navigation state while offering detailed feedback on issues.

## Usage:
This type is ideal for scenarios requiring:

* Automated web testing with complex interaction sequences.
* Debugging and validation of UI workflows with visual feedback.
* Clear and maintainable definitions of navigation flows in browser-based environments.

By encapsulating navigation logic, browser state, and interaction results in an immutable structure, NavigationProtocol promotes best practices in test automation and browser interaction, ensuring consistency and reducing the likelihood of runtime errors.


# License

`Selenium-Navigation-Protocol` is an open source software released under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0).
