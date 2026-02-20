# PR Review and Management System
==================================

This is a monolithic Spring Boot application for managing Pull Requests (PRs), code reviews, and role-based developer workflows. It is configured to run locally with zero external database dependencies.

## Key Features
- **Role-Based Access Control**: Admin, Team Lead, Reviewer, and Developer roles.
- **PR Lifecycle Management**: Create, view, edit (open only), approve, reject, and merge PRs.
- **Audit Logging**: Tracks all PR state transitions securely.
- **In-Memory Database**: Utilizes an H2 in-memory database that wipes data on shutdown, making it ideal for immediate local testing without requiring MySQL installation.
- **Self-Seeding**: The application automatically seeds 4 user accounts and 6 test pull requests with fake audit logs and reviews on startup.

## Prerequisites
- Java 25 (Project configured for Java 25 compatibility)
- Maven (Wrapper is included in the project)

## Getting Started

Follow these instructions to build and run the application locally.

### 1. Build the Project
Open a terminal in the root of the project directory and run:
```bash
./mvnw clean install -DskipTests
```

### 2. Run the Application
Start the Spring Boot application using the Maven wrapper:
```bash
./mvnw spring-boot:run
```

The application will start on port `8080` internally and automatically seed the database.

### 3. Access the Application
Open your web browser and navigate to:
[http://localhost:8080](http://localhost:8080)

You will be redirected to the login page.

## Test Credentials
You can log in with any of these seeded accounts (Password for all: `password123`):

*   **Admin**: `admin@test.com` (Has full access to `/admin` and all PRs)
*   **Team Lead**: `lead@test.com` (Can approve/reject any PR, view audit logs)
*   **Reviewer**: `reviewer@test.com` (Can submit reviews on assigned PRs)
*   **Developer**: `dev@test.com` (Can create PRs and view their own)

## Database Console (H2)
You can view the in-memory database directly through the H2 Console while the application is running:

1.  Navigate to [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
2.  **JDBC URL**: `jdbc:h2:mem:pr_review_db`
3.  **User Name**: `sa`
4.  **Password**: *(leave blank)*
5.  Click **Connect**

## Troubleshooting
*   **Port in use**: If the application fails to start because port 8080 is already in use, you can change the port in `src/main/resources/application.properties` by updating `server.port=8080`.
*   **Mail Configuration**: Mail auto-configuration has been explicitly disabled (`spring.autoconfigure.exclude=...MailAutoConfiguration`) to allow testing without an SMTP server. Ensure it remains disabled unless providing real credentials.
