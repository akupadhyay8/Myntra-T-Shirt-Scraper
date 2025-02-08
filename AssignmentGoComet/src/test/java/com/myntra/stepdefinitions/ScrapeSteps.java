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
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        page.navigate("https://www.myntra.com/");
        System.out.println("✅ Opened Myntra homepage");
    }

    @When("I select {string} category and {string} subcategory")
    public void select_category(String category, String subcategory) {
        selectedCategory = subcategory;
        page.hover("//a[text()='" + category + "']");
        page.waitForTimeout(1000);
        page.click("//a[text()='" + subcategory + "']");
        System.out.println("✅ Navigated to " + category + " > " + subcategory);
    }

    @When("I filter by brand {string}")
    public void filter_brand(String brand) {
        page.click("//span[text()='Brand']");
        page.waitForTimeout(1000);

        if (page.isVisible(".filter-search-iconSearch")) {
            page.click(".filter-search-iconSearch");
        }
        
        Locator searchBox = page.locator("//input[@placeholder='Search for Brand']");
        searchBox.click();
        searchBox.fill(brand);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");

        Locator brandLabel = page.locator("//label[normalize-space()='" + brand + "']");
        if (brandLabel.isVisible()) {
            brandLabel.click();
            System.out.println("✅ Applied brand filter: " + brand);
        } else {
            System.out.println("❌ Brand '" + brand + "' not found!");
        }

        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);
    }

    @When("I scrape discounted products from all pages")
    public void scrape_data() {
        productData.clear(); // Clear previous data
        System.out.println("🔄 Starting data scraping...");
        
        while (true) {
            page.waitForSelector(".product-base");
            List<ElementHandle> products = page.querySelectorAll(".product-base");
            
            for (ElementHandle product : products) {
                ElementHandle discountElement = product.querySelector(".product-discountPercentage");
                if (discountElement != null) {
                    String discount = discountElement.innerText().trim();
                    String price = product.querySelector(".product-price").innerText().trim();
                    String link = product.querySelector("a").getAttribute("href");
                    
                    productData.add(Map.of(
                        "price", price,
                        "discount", discount,
                        "link", "https://www.myntra.com" + link
                    ));
                }
            }

            ElementHandle nextButton = page.querySelector(".pagination-next");
            if (nextButton == null || !nextButton.isEnabled()) break;
            nextButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }

        System.out.println("✅ Scraped " + productData.size() + " products.");
    }

    @Then("I print the scraped data sorted by highest discount")
    public void print_sorted_data() {
        Assertions.assertFalse(productData.isEmpty(), "No discounted products were scraped!");

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

        browser.close();
        playwright.close();
        System.out.println("✅ Browser closed.");
    }
}
