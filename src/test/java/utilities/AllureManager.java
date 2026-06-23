package utilities;
import java.io.ByteArrayInputStream;
import io.qameta.allure.Allure;
public class AllureManager {
    public static void attachScreenShot(String screenshotName){
        Allure.addAttachment(screenshotName, new ByteArrayInputStream(Capture.captureScreenshot()));
    
    }
}