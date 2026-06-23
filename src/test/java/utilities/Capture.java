package utilities;
import commons.DriverManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.io.FileHandler;

public class Capture {
    private static final Logger log = LogManager.getLogger(Capture.class);
    public static void captureScreenshot(String screenshotName) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Cannot capture screenshot because driver is null");
            return;
        }
        File capture = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File screenShotFile = new File("images/" + screenshotName + ".png");
        try{
         FileHandler.copy(capture, screenShotFile);
        log.info("Screenshot capture successful " + screenShotFile.getAbsolutePath());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    }
    public static byte []captureScreenshot(){
        return ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
    }
}