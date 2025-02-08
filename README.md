# 🛒 Myntra T-Shirt Scraper with Playwright & Cucumber BDD (Java)

This project is an automated web scraping tool built using Playwright and Cucumber BDD in Java. The objective is to navigate Myntra.com, apply category and brand filters, scrape discounted T-shirts, and sort them by the highest discount. The framework follows Behavior-Driven Development (BDD) principles, making the test scenarios structured, readable, and reusable.

---

## 🎯 **Overview**
_A modern browser automation tool to extract discounted T-shirt data from Myntra.com_  
✔️ Filter by category & brand  
✔️ Multi-page scraping capability  
✔️ Smart sorting by discounts  
✔️ BDD-structured test scenarios

## 🏗 **Tech Stack**

- **Java (JDK 21)** – Core programming language.  
- **Playwright** – Modern browser automation.  
- **Cucumber BDD** – Test scenarios in Gherkin syntax.  
- **JUnit 5** – Test execution framework.  
- **Maven** – Dependency management & build tool.

## 🗂 **Project Structure**
📂 MyntraTShirtScraper
┣ 📂 src/test/java/com/myntra
┃ ┣ 📂 stepdefinitions # Cucumber step definitions
┃ ┣ 📂 runner # TestRunner class for execution
┃ ┗ 📂 utilities # Helper methods for data extraction
┣ 📂 src/test/resources/features
┃ ┗ 📜 scrape_discounted_t_shirts.feature # Gherkin feature files
┣ 📄 pom.xml # Project dependencies (Maven)
┗ 📄 README.md # Project Documentation


