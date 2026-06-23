# 🧪 Selenium Java Automation Framework

A web automation testing framework built with **Java** and **Selenium WebDriver**, designed following the **Page Object Model (POM)** architecture for better maintainability, scalability, and reusability.

---

# 🚀 Tech Stack

| Category | Technology |
|----------|-------------|
| Programming Language | Java |
| Automation Tool | Selenium WebDriver |
| Test Framework | TestNG |
| Build Tool | Maven |
| Reporting | Allure Report |
| CI/CD | GitHub Actions |
| Version Control | Git & GitHub |
---

# 📂 Project Structure

```bash
📦 selenium-java-framework
├── 📂 src/main/java
│ ├── 📂 commons
│ │ ├── BasePage.java 
│ │ ├── DriverManager.java 
│ │ └── LocatorType.java 
│ ├── 📂 pageAction 
│ ├── 📂 pageUI 
├── 📂 src/test/java
│ ├── 📂 commons
│ │ └── BaseTest.java 
│ ├── 📂 helpers 
│ ├── 📂 listener 
│ ├── 📂 step # Test case implementations (TestNG)
│ ── 📂 pageVerification 
├── 📂 src/test/resources
│ ├── log4j2.xml # Logging configuration
│ └── *.xml # TestNG suite files (swaglab.xml, slidedemo.xml...)
├── 📄 pom.xml # Dependencies: Selenium, TestNG, Log4j2, etc.
├── 📄 github/workflows/main.yml # CI/CD pipeline config
└── 📄 .gitignore
```

- **testcases** → Contains automated test scripts  
- **pageObjects** → Stores Page Object classes  
- **commons** → Shared utilities and base classes  
- **utilities** → Helper functions 
---

# ⚙️ Installation & Local Execution

## 1️⃣ Clone Repository

```bash
git clone <repository-url>
```

## 2️⃣ Navigate to Project Directory

Make sure your terminal is pointing to the folder containing the `pom.xml` file.

```bash
cd selenium-java-framework-1
```

## 3️⃣ Install Dependencies & Run Tests

The following command will:

- Clean previous build files
- Download dependencies
- Compile source code
- Execute all automated test cases

```bash
mvn clean verify
```
---

# 📊 Generate Allure Report

After test execution, Allure raw result files will be generated inside:

```bash
target/allure-results
```

To start the Allure server and view the interactive HTML report:

```bash
allure serve target/allure-results
```

> 💡 Press `Ctrl + C` in Terminal to stop the Allure server after viewing the report.
---

# 🤖 CI/CD Integration with GitHub Actions

This project is integrated with **GitHub Actions** for Continuous Integration and automated test execution.

Workflow configuration is located at:

```bash
.github/workflows/main.yml
```

## ▶️ Run Workflow Manually

1. Open the repository on GitHub  
2. Navigate to the **Actions** tab  
3. Select workflow: **Java Test and Allure Report**  
4. Click **Run workflow**  

GitHub Actions will automatically:
- Provision a virtual environment
- Build the project
- Execute automated tests
- Generate execution logs and reports
---

# ✅ Automation Features

- Web UI Automation Testing
- Page Object Model (POM)
- Explicit Wait Handling
- XPath & CSS Locator Strategy
- Cross-page Reusable Components
- Allure Reporting Integration
- CI/CD Pipeline Execution
- Maven-based Project Management
---

# 📌 Future Enhancements

- Cross-browser Testing
- Parallel Test Execution
- Docker Integration
- Jenkins CI Pipeline
- Data-Driven Testing
- API Automation Integration
---

# 👩‍💻 Author

- 💻 Maintained by: [Ly Huyen Trang]
- 📧 Contact: [lyhuyentrang1202@gmail.com]
- 🐙 GitHub: [https://github.com/lyhuyentrang1202-QC]
---

# ⭐ Repository Purpose

This repository is created for:

- Practicing automation testing skills
- Building reusable automation frameworks
- Demonstrating Selenium & Java knowledge
- Learning CI/CD integration workflow
- Portfolio & technical showcase
---