package com.valdisdot.jobscrapper.util;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.function.Supplier;

public class WebDriverFactories {
    public static Supplier<WebDriver> webDriver(String type) {
        if (type != null) {
            switch (type.toLowerCase()) {
                case "chrome": {
                    return () -> {
                        ChromeOptions options = new ChromeOptions();
                        options.addArguments("--headless=new");
                        options.addArguments("--no-sandbox");
                        options.addArguments("--window-size=1920,1080");
                        options.addArguments("--disable-gpu");
                        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

                        ChromeDriver driver = new ChromeDriver(options);

                        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
                        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
                        driver.manage().window().setSize(new Dimension(1920, 1080));
                        return driver;
                    };
                }

                case "firefox": {
                    return () -> {
                        FirefoxOptions options = new FirefoxOptions();
                        options.addArguments("--headless");
                        options.addArguments("--width=1920");
                        options.addArguments("--height=1080");
                        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

                        FirefoxDriver driver = new FirefoxDriver(options);

                        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
                        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
                        driver.manage().window().setSize(new Dimension(1920, 1080));
                        return driver;
                    };
                }
            }
            throw new RuntimeException("Unknown driver type: " + type);
        }
        throw new NullPointerException("Driver type is null");
    }
}
