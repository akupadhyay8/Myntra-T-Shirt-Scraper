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
        // Start Playwright and launch a Chromium browser instnce
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));  // headless mode is off for debugging
        page = browser.newPage();
        
        // Open Myntra
        page.navigate("https://www.myntra.com/");
        System.out.println("✅ Opened Myntra homepage");
    }

    @When("I select {string} category and {string} subcategory")
    public void select_category(String category, String subcategory) {
        selectedCategory = subcategory; // Storing subcategory for later use
        
        // Hover over the category ("Men") to reveal the subcategories
        page.hover("//a[text()='" + category + "']");
        page.waitForTimeout(1000); // Small delay to ensure the dropdown loads properly
        
        // Click on the subcategory ("T-Shirts")
        page.click("//a[text()='" + subcategory + "']");
        System.out.println("✅ Navigated to " + category + " > " + subcategory);
    }

    @When("I filter by brand {string}")
    public void filter_brand(String brand) {
        // Open the "Brand" filter section
        page.click("//span[text()='Brand']");
        page.waitForTimeout(1000);

        // Click the search box if it exists (helps when there are many brands)
        if (page.isVisible(".filter-search-iconSearch")) {
            page.click(".filter-search-iconSearch");
        }
        
        // Enter the brand name in the search box
        Locator searchBox = page.locator("//input[@placeholder='Search for Brand']");
        searchBox.click();
        searchBox.fill(brand);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");

        // Locate the brand checkbox and apply the filter
        Locator brandLabel = page.locator("//label[normalize-space()='" + brand + "']");
        if (brandLabel.isVisible()) {
            brandLabel.click();
            System.out.println("✅ Applied brand filter: " + brand);
        } else {
            System.out.println("Brand '" + brand + "' found!");
        }

        // Wait for the page to fully load after applying the filter
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);
    }

    @When("I scrape discounted products from all pages")
    public void scrape_data() {
        productData.clear(); // Clear any previously stored data
        System.out.println("🔄 Starting data scraping...");
        
        // Loop until we collect 150 discounted products or there are no more pages
        while (productData.size() < 150) {
            page.waitForSelector(".product-base"); // Ensure products are loaded
            List<ElementHandle> products = page.querySelectorAll(".product-base");

            for (ElementHandle product : products) {
                if (productData.size() >= 150) break; // Stop once we reach 150 products
                
                // Check if the product has a discount
                ElementHandle discountElement = product.querySelector(".product-discountPercentage");
                if (discountElement != null) {
                    String discount = discountElement.innerText().trim();
                    String price = product.querySelector(".product-price").innerText().trim();
                    String link = product.querySelector("a").getAttribute("href");

                    // Store product details
                    productData.add(Map.of(
                        "price", price,
                        "discount", discount,
                        "link", "https://www.myntra.com" + link
                    ));
                }
            }

            // Breaking look
            if (productData.size() >= 150) break;

            // Move to the next page if "Next" button available
            ElementHandle nextButton = page.querySelector(".pagination-next");
            if (nextButton == null || !nextButton.isEnabled()) break;
            nextButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }

        System.out.println("✅ Scraped " + productData.size() + " products (Max 150).");
    }

    @Then("I print the scraped data sorted by highest discount")
    public void print_sorted_data() {
        // If no products scraped, stop execution
        Assertions.assertFalse(productData.isEmpty(), "No discounted products were scraped!");

        // Sort products by discount percentage in descending order
        productData.sort((a, b) -> {
            int discountA = Integer.parseInt(a.get("discount").replaceAll("[^0-9]", ""));
            int discountB = Integer.parseInt(b.get("discount").replaceAll("[^0-9]", ""));
            return Integer.compare(discountB, discountA);
        });

        // Printing sorted data
        System.out.println("\n--- Discounted " + selectedCategory + " (Sorted by Discount) ---");
        for (Map<String, String> data : productData) {
            System.out.printf("Price: %s | Discount: %s | Link: %s%n",
                    data.get("price"), data.get("discount"), data.get("link"));
        }

        // Close the browser now
        browser.close();
        playwright.close();
        System.out.println("✅ Browser closed.");
    }
}
