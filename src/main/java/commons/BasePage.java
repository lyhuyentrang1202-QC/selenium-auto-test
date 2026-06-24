package commons;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BasePage {

    public final Duration timeOut = Duration.ofSeconds(30);

    protected WebElement findElement(LocatorType type, String element) {
        try {
            WebElement webElement = null;
            switch (type) {
                case Xpath -> webElement = DriverManager.getDriver().findElement(By.xpath(element));
                case CssSelector -> webElement = DriverManager.getDriver().findElement(By.cssSelector(element));
            }
            return webElement;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error finding element with %s: '%s' - %s", type, element, e.getMessage()), e); 
                    // "Error finding element with %s:'%s' - %s" Có 3 %s: -> Nó sẽ được thay bằng: (type, element,e.getMessage())
        }
    }

    protected void waitElementToVisible(LocatorType type, String element) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), timeOut);
            wait.until(ExpectedConditions.visibilityOf(findElement(type, element)));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Timeout (%ds) waiting for element to be visible with %s: '%s'",
                            timeOut.getSeconds(), type, element));
        } catch (RuntimeException e) {
            throw e;
        }
    }

    protected void waitElementToClickable(LocatorType type, String element) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), timeOut);
            wait.until(ExpectedConditions.elementToBeClickable(findElement(type, element)));
        } catch (TimeoutException e) {
            throw new TimeoutException(
                    String.format("Timeout (%ds) waiting for element to be clickable with %s: '%s'",
                            timeOut.getSeconds(), type, element));
        } catch (RuntimeException e) {
            throw e;
        }
    }

    protected void clickElement(LocatorType type, String element) {
        waitElementToClickable(type, element);
        findElement(type, element).click();
    }

    protected void sendKeysElement(LocatorType type, String element, String key) {
        waitElementToVisible(type, element);
        findElement(type, element).sendKeys(key);
    }

    protected String getCurrentUrl() {
        return DriverManager.getDriver().getCurrentUrl();
    }

    protected String getTextElement(LocatorType type, String element) {
        waitElementToVisible(type, element);
        return findElement(type, element).getText();
    }
}
