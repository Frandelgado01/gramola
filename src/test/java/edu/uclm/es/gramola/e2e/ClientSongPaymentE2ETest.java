package edu.uclm.es.gramola.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Selenium E2E scenarios requested:
 * 1) Search a song, pay successfully, verify payment in DB and song added to backend queue.
 * 2) Search a song, enter wrong payment data, verify error and song NOT added.
 *
 * Prerequisites (recommended):
 * - Start frontend+backend with E2E enabled: `npm run dev:e2e` from the frontend folder.
 * - Then run these tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientSongPaymentE2ETest {

    private static final String DEFAULT_FRONTEND_URL = "http://localhost:4200";
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";
    private static final String E2E_EMAIL = "e2e@example.com";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private WebDriver driver;
    private WebDriverWait wait;
    private String frontendUrl;
    private String backendUrl;

    @BeforeAll
    void setupSuite() throws Exception {
        Assumptions.assumeTrue(isSeleniumE2eEnabled(),
            "Selenium E2E is disabled. Enable with env E2E_SELENIUM=true (recommended on PowerShell) or JVM -De2e.selenium=true");

        frontendUrl = System.getProperty("e2e.frontendUrl", DEFAULT_FRONTEND_URL);
        backendUrl = System.getProperty("e2e.backendUrl", DEFAULT_BACKEND_URL);

        // Require E2E endpoints to be enabled.
        HttpResponse<String> health = httpGet(backendUrl + "/e2e/health");
        Assumptions.assumeTrue(health.statusCode() == 200,
                "E2E endpoints not available. Start backend with e2e.enabled=true (env: E2E_ENABLED=true). Response="
                        + health.statusCode());

        resetBackendState();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1280,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);

        wait = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    private static boolean isSeleniumE2eEnabled() {
        String prop = System.getProperty("e2e.selenium");
        if (prop != null && prop.equalsIgnoreCase("true")) {
            return true;
        }
        String env = System.getenv("E2E_SELENIUM");
        return env != null && env.equalsIgnoreCase("true");
    }

    @AfterAll
    void teardownSuite() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void resetStateBeforeEach() throws Exception {
        resetBackendState();
        bootstrapFrontendSession();
    }

    @Test
    void scenario1_successfulPayment_addsSong_andPaymentConfirmedInDb() throws Exception {
        String query = "rock";
        searchSong(query);
        clickFirstSongPayButton();

        payWithCard("4242424242424242", "12/34", "123", "12345");

        // Some environments (headless) can be flaky with JS alerts. Accept alert if present,
        // but also allow success to be detected by navigation to /home.
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(70));
        longWait.until(d -> {
            try {
                d.switchTo().alert();
                return true;
            } catch (Exception ignore) {
                return d.getCurrentUrl() != null && d.getCurrentUrl().contains("/home");
            }
        });

        try {
            Alert alert = driver.switchTo().alert();
            assertNotNull(alert.getText());
            alert.accept();
        } catch (Exception ignore) {
            // no alert; success detected by navigation
        }

        longWait.until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains("/home"));

        JsonNode state = e2eState(E2E_EMAIL);
        assertEquals(1, state.get("songCount").asInt(), "Song should be added to backend queue");
        assertTrue(state.get("transactionCount").asInt() >= 1, "Transaction should exist in DB");

        JsonNode songs = state.get("songs");
        JsonNode song0 = songs.get(0);
        assertEquals("E2E Song: " + query, song0.get("title").asText());
        assertEquals("E2E Artist", song0.get("artist").asText());

        // Confirm payment in backend using last transaction id for the email.
        List<String> txIds = jsonStringList(state.get("transactionIds"));
        String txId = txIds.get(txIds.size() - 1);
        HttpResponse<String> confirm = httpPostJson(backendUrl + "/gramola/payments/confirm", MAPPER
            .writeValueAsString(Map.of("transactionId", txId)));
        assertEquals(200, confirm.statusCode(), "Payment should be confirmed by backend");
        JsonNode confirmJson = MAPPER.readTree(confirm.body());
        assertTrue(confirmJson.get("ok").asBoolean(), "Confirm response should be ok=true");
    }

    @Test
    void scenario2_wrongPaymentData_showsError_andDoesNotAddSong() throws Exception {
        String query = "pop";
        searchSong(query);
        clickFirstSongPayButton();

        // Declined test card number.
        payWithCard("4000000000000002", "12/34", "123", "12345");

        WebElement cardErrors = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#card-errors")));
        wait.until(d -> {
            String text = cardErrors.getText();
            return text != null && !text.trim().isEmpty();
        });

        String err = cardErrors.getText().toLowerCase();
        assertFalse(err.isBlank(), "Should display a Stripe error message");
        // Depending on focus/Stripe validation, the error can be "declined" or a validation message like "incomplete".
        assertTrue(
            err.contains("declin") || err.contains("deneg") || err.contains("rejected") || err.contains("invalid")
                || err.contains("rechaz") || err.contains("incomplet") || err.contains("incomplete"),
            "Expected a payment error message, got: " + cardErrors.getText());

        JsonNode state = e2eState(E2E_EMAIL);
        assertEquals(0, state.get("songCount").asInt(), "Song should NOT be added when payment fails");

        // If a transaction exists (prepay runs), confirm should fail.
        if (state.get("transactionCount").asInt() > 0) {
            List<String> txIds = jsonStringList(state.get("transactionIds"));
            String txId = txIds.get(txIds.size() - 1);
            HttpResponse<String> confirm = httpPostJson(backendUrl + "/gramola/payments/confirm",
                    MAPPER.writeValueAsString(Map.of("transactionId", txId)));
                assertTrue(confirm.statusCode() == 402 || confirm.statusCode() == 403 || confirm.statusCode() == 400,
                    "Expected payment confirmation to fail (402/403/400). Got: " + confirm.statusCode());
        }
    }

    private void bootstrapFrontendSession() {
        driver.get(frontendUrl + "/login");

        String usuarioJson;
        try {
            usuarioJson = MAPPER.writeValueAsString(
                    Map.of("email", E2E_EMAIL, "bar", "E2E Bar", "subscriptionActive", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ((JavascriptExecutor) driver).executeScript("window.localStorage.setItem('e2e_mode','true');");
        ((JavascriptExecutor) driver).executeScript("window.localStorage.setItem('spotify_access_token','e2e-token');");
        ((JavascriptExecutor) driver).executeScript("window.localStorage.setItem('usuario_gramola', arguments[0]);",
            usuarioJson);

        driver.get(frontendUrl + "/home");

        // Load devices in E2E mode (mocked).
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-refresh"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Buscar canción...']")));
    }

    private void searchSong(String query) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[placeholder='Buscar canción...']")));
        input.clear();
        input.sendKeys(query);
        input.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".track-item")));
    }

    private void clickFirstSongPayButton() {
        WebElement payBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".track-item .btn-add")));
        payBtn.click();
        wait.until(ExpectedConditions.urlContains("/payment"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".payment-container")));
    }

    private void payWithCard(String number, String exp, String cvc, String postal) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#card-element iframe")));
        WebElement iframe = driver.findElement(By.cssSelector("#card-element iframe"));

        driver.switchTo().frame(iframe);

        WebElement cardNumber = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='cardnumber'], input[autocomplete='cc-number']")));
        cardNumber.click();
        cardNumber.sendKeys(number);

        // Stripe Elements usually exposes these named inputs in the same iframe.
        WebElement expDate = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='exp-date'], input[autocomplete='cc-exp']")));
        expDate.sendKeys(exp);

        WebElement cvcInput = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='cvc'], input[autocomplete='cc-csc']")));
        cvcInput.sendKeys(cvc);

        // Postal/ZIP can be optional depending on Stripe config; fill if present.
        List<WebElement> postalInputs = driver.findElements(By.cssSelector("input[name='postal'], input[autocomplete='postal-code']"));
        if (!postalInputs.isEmpty()) {
            WebElement postalInput = postalInputs.get(0);
            try {
                postalInput.sendKeys(postal);
            } catch (Exception ignore) {
            }
        }
        driver.switchTo().defaultContent();

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-pay"))).click();
    }

    private void resetBackendState() throws Exception {
        HttpResponse<String> res = httpPostJson(backendUrl + "/e2e/reset", "{}");
        Assumptions.assumeTrue(res.statusCode() == 200,
                "Failed to reset backend state via /e2e/reset. Status=" + res.statusCode() + " body=" + res.body());
    }

    private JsonNode e2eState(String email) throws Exception {
        HttpResponse<String> res = httpGet(backendUrl + "/e2e/state?email=" + urlEncode(email));
        assertEquals(200, res.statusCode(), "Expected /e2e/state to be reachable");
        return MAPPER.readTree(res.body());
    }

    private HttpResponse<String> httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> httpPostJson(String url, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static List<String> jsonStringList(JsonNode arrayNode) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return out;
        }
        for (JsonNode n : arrayNode) {
            out.add(n.asText());
        }
        return out;
    }
}
