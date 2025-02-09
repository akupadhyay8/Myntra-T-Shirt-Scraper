package com.myntra.stepdefinitions;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import java.util.*;

public class ScrapeSteps {
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private List<Map<String, String>> productData = new ArrayList<>();
    private String selectedCategory = "";

    @Given("I open Myntra.com")
    public void open_myntra() {
        // Initialize Playwright and opn browser
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        page.navigate("https://www.myntra.com/");
        System.out.println("✅ Opened Myntra homepage");
    }

    @When("I select {string} category and {string} subcategory")
    public void select_category(String category, String subcategory) {
        // Storing subcategory for later
        selectedCategory = subcategory;
        
        // Hover main category and clicking subcategory
        page.hover("//a[text()='" + category + "']");
        page.waitForTimeout(1000);
        page.click("//a[text()='" + subcategory + "']");
        System.out.println("✅ Navigated to " + category + " > " + subcategory);
    }

    @When("I filter by brand {string}")
    public void filter_brand(String brand) {
        // Opening brand filter
        page.click("//span[text()='Brand']");
        page.waitForTimeout(1000);

        // Check if the brand search input is available and use it
        if (page.isVisible(".filter-search-iconSearch")) {
            page.click(".filter-search-iconSearch");
        }
        
        // interacting with the search box to enter brnd name
        Locator searchBox = page.locator("//input[@placeholder='Search for Brand']");
        searchBox.click();
        searchBox.fill(brand);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");

        // Selecting brand if it's visible
        Locator brandLabel = page.locator("//label[normalize-space()='" + brand + "']");
        if (brandLabel.isVisible()) {
            brandLabel.click();
            System.out.println("✅ Applied brand filter: " + brand);
        } else {
            System.out.println("Brand '" + brand + "' found!");
        }

        // Ensure the filters are applied properly
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);
    }

    @When("I scrape discounted products from all pages")
    public void scrape_data() {
        // Clear any previous data before starting
        productData.clear();
        System.out.println("🔄 Starting data scraping...");
        
        while (productData.size() < 250) {  // only collecting 250 products
            page.waitForSelector(".product-base");
            List<ElementHandle> products = page.querySelectorAll(".product-base");

            for (ElementHandle product : products) {
                if (productData.size() >= 250) break; // Stop if we reach 250 products
                
                ElementHandle discountElement = product.querySelector(".product-discountPercentage");
                if (discountElement != null) {
                    // Extracting discount, price, and product link
                    String discount = discountElement.innerText().trim();
                    String price = product.querySelector(".product-price").innerText().trim();
                    String link = product.querySelector("a").getAttribute("href");

                    // Ensure the link starts with "/" to avoid malformed URLs
                    if (link != null && !link.startsWith("/")) {
                        link = "/" + link;
                    }

                    // Store product details in a list
                    productData.add(Map.of(
                        "price", price,
                        "discount", discount,
                        "link", "https://www.myntra.com" + link
                    ));
                }
            }

            // If we reached 250 products, exit the loop
            if (productData.size() >= 250) break;

            // Navigating the next page if next button exists
            ElementHandle nextButton = page.querySelector(".pagination-next");
            if (nextButton == null || !nextButton.isEnabled()) break;
            nextButton.click();
            
            // Wait for the next page to fully load before scraping again
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);
        }

        System.out.println("✅ Scraped " + productData.size() + " products (Max 250).");
    }

    @Then("I print the scraped data sorted by highest discount")
    public void print_sorted_data() {
        // Ensure that some data has been scraped before proceeding
        Assertions.assertFalse(productData.isEmpty(), "No discounted products were scraped!");

        // Sort the scraped products based on the discount percentage (highest first)
        productData.sort((a, b) -> {
            int discountA = Integer.parseInt(a.get("discount").replaceAll("[^0-9]", ""));
            int discountB = Integer.parseInt(b.get("discount").replaceAll("[^0-9]", ""));
            return Integer.compare(discountB, discountA);
        });

        // Print the sorted results
        System.out.println("\n--- Discounted " + selectedCategory + " (Sorted by Discount) ---");
        for (Map<String, String> data : productData) {
            System.out.printf("Price: %s | Discount: %s | Link: %s%n",
                    data.get("price"), data.get("discount"), data.get("link"));
        }

        // Close the browser and Playwright after scraping is complete
        browser.close();
        playwright.close();
        System.out.println("✅ Browser closed.");
    }
}
