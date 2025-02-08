Feature: Scrape Discounted Products on Myntra

  Scenario: Scrape and Print Discounted T-Shirts
    Given I open Myntra.com
    When I select "Men" category and "T-Shirts" subcategory
    And I filter by brand "Van Heusen"
    And I scrape discounted products from all pages
    Then I print the scraped data sorted by highest discount

  Scenario: Scrape and Print Discounted Casual Shirts
    Given I open Myntra.com
    When I select "Men" category and "Casual Shirts" subcategory
    And I filter by brand "Roadster"
    And I scrape discounted products from all pages
    Then I print the scraped data sorted by highest discount
