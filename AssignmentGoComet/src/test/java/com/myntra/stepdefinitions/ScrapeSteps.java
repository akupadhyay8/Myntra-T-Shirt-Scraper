package com.myntra.stepdefinitions;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import java.util.*;

public class ScrapeSteps {
    private Playwright playwright;  // Playwright instance
    private Browser browser;  // Browser instance
    private Page page;  // Page instance
    private List<Map<String, String>> productData = new ArrayList<>();  // List to storing scraped product data
    private String selectedCategory = "";  

    // fun for open Myntra's homepage
    @Given("I open Myntra.com")
    public void open_myntra() {
        // Launching the browser and open the wbsite
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));  // Open in non-headless mode to see the browser
        page = browser.newPage();  // Create a new browser tab
        page.navigate("https://www.myntra.com/");  
        System.out.println("✅ Opened Myntra homepage");
        // Validate that the page URL is Myntra
        Assertions.assertTrue(page.url().contains("myntra.com"), "❌ Failed to load Myntra homepage!");
    }

    // fun for select a category and subcategory from the homepage
    @When("I select {string} category and {string} subcategory")
    public void select_category(String category, String subcategory) {
        selectedCategory = subcategory; 
        // Hover over the main category and click on the subcategory
        page.hover("//a[text()='" + category + "']");
        page.waitForTimeout(1000);  
        page.click("//a[text()='" + subcategory + "']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        System.out.println("✅ Navigated to " + category + " > " + subcategory);
        page.waitForSelector(".product-base", new Page.WaitForSelectorOptions().setTimeout(5000));
        // Verify if the product elements are visible on the page
        Assertions.assertTrue(page.isVisible(".product-base"),
            "❌ Products are not visible on the " + subcategory + " page!");
    }

    // fun for filter products by brand
    @When("I filter by brand {string}")
    public void filter_brand(String brand) {
        // Open the brand filter section and search for the brand
        page.click("//span[text()='Brand']");
        page.waitForTimeout(1000);
        
        if (page.isVisible(".filter-search-iconSearch")) {
            page.click(".filter-search-iconSearch");
            page.waitForTimeout(500);
        }
        // Locate the search box and type the brand name
        Locator searchBox = page.locator("//input[@placeholder='Search for Brand']");
        searchBox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchBox.click();
        searchBox.fill(brand);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");
        page.waitForTimeout(3000);
        // Locate and click on the brand filter checkbox if available
        Locator brandLabel = page.locator("//label[contains(@class, 'common-customCheckbox') and contains(normalize-space(), '" + brand + "')]"); 
        if (brandLabel.count() > 0) {
            brandLabel.first().click();
            System.out.println("✅ Applied brand filter: " + brand);
        } else {
            // If the brand filter checkbox is not found, the test will fail
            Assertions.fail("❌ Brand '" + brand + "' label not found!");
        }
        // Wait for the page to load after applying the filter
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);  // Waiting for aditionla loading
    }

    // fun for attempt applying a brand filter
    @When("I attempt to filter by brand {string}")
    public void attempt_filter_brand(String brand) {
        // Same process as the previous step
        page.click("//span[text()='Brand']");
        page.waitForTimeout(1000);
        if (page.isVisible(".filter-search-iconSearch")) {
            page.click(".filter-search-iconSearch");
            page.waitForTimeout(500);
        }
        Locator searchBox = page.locator("//input[@placeholder='Search for Brand']");
        searchBox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        searchBox.click();
        searchBox.fill(brand);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");
        page.waitForTimeout(3000);
        // Try to locate the brand checkbox, but don't fail if it doesn't exist
        Locator brandLabel = page.locator("//label[contains(@class, 'common-customCheckbox') and contains(normalize-space(), '" + brand + "')]"); 
        if (brandLabel.count() > 0) {
            System.out.println("❌ Unexpectedly found brand filter for: " + brand);
            brandLabel.first().click();  // If found, click it
        } else {
            System.out.println("✅ Brand '" + brand + "' not found, as expected.");
        }
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);
    }

    // Step to scrape data from all pages of the prodcts
    @When("I scrape discounted products from all pages")
    public void scrape_data() {
        productData.clear();  // Clear any previously scraped data
        System.out.println("🔄 Starting data scraping...");
        // Loop to scrape at maximaum up to 250 products
        while (productData.size() < 250) {
            page.waitForSelector(".product-base", new Page.WaitForSelectorOptions().setTimeout(5000));  // Wait for product base elements to load
            List<ElementHandle> products = page.querySelectorAll(".product-base");  // Get all product elements
            for (ElementHandle product : products) {
                if (productData.size() >= 250) break;  // Stop if it reached 250 products
                // Check for discount element and scrape
                ElementHandle discountElement = product.querySelector(".product-discountPercentage");
                if (discountElement != null) {
                    String discount = discountElement.innerText().trim();  // Getting the discount text
                    String price = product.querySelector(".product-price").innerText().trim();  // Getting the price text
                    String link = product.querySelector("a").getAttribute("href");  // Getting product link
                    if (link != null && !link.startsWith("/")) {
                        link = "/" + link;  // link starts with a slash
                    }
                    // Adding scraped data into list
                    productData.add(Map.of(
                        "price", price,
                        "discount", discount,
                        "link", "https://www.myntra.com" + link
                    ));
                }
            }
            
            if (productData.size() >= 250) break;
            ElementHandle nextButton = page.querySelector(".pagination-next");  // Locate next page button
            if (nextButton == null || !nextButton.isEnabled()) break;  // Exit if no next button
            nextButton.click();  // Click the next button
            page.waitForLoadState(LoadState.NETWORKIDLE);  
            page.waitForTimeout(2000);
        }
        // Output the number of products scraped
        System.out.println("✅ Scraped " + productData.size() + " products (Max 250).");
        Assertions.assertFalse(productData.isEmpty(), "❌ No products were scraped!");
    }

    // fun for printint the scraped data and sorted by highest discount first
    @Then("I print the scraped data sorted by highest discount")
    public void print_sorted_data() {
        Assertions.assertFalse(productData.isEmpty(), "❌ No discounted products were scraped!");
        
        productData.sort((a, b) -> {
            int discountA = Integer.parseInt(a.get("discount").replaceAll("[^0-9]", ""));
            int discountB = Integer.parseInt(b.get("discount").replaceAll("[^0-9]", ""));
            return Integer.compare(discountB, discountA);
        });
        
        System.out.println("\n--- Discounted " + selectedCategory + " (Sorted by Discount) ---");
        for (Map<String, String> data : productData) {
            System.out.printf("Price: %s | Discount: %s | Link: %s%n",
                    data.get("price"), data.get("discount"), data.get("link"));
        }
        // Closing the brwoser after the task is complete
        browser.close();
        playwright.close();
        System.out.println("✅ Browser closed.");
    }

    // verifing that the brand filter applied for invalid brands
    @Then("I verify that the brand filter did not apply")
    public void verify_brand_filter_not_applied() {
        // Assert that no products were scraped, as the filter was invalid
        Assertions.assertTrue(productData.isEmpty(), "Brand filter unexpectedly applied for invalid brand!");
        System.out.println("✅ No products found for the invalid brand filter. Closing browser.");
        browser.close();
        playwright.close();
    }
}
