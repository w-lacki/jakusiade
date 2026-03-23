# jakusiade

Jakusiade is a web application that helps travelers find train connections and the best available seats.

The main idea behind this project is to solve a common problem: when a seat is not available for your entire trip (from the start to the end station), you usually have to buy a ticket without a seat guarantee and stand for the whole journey.\
This app checks the route stop-by-stop and finds an empty seat for the longest possible part of your journey, maximizing the duration you are able to sit.

## Features

* **Station and Route Search:** Find stations and train connections using data from the Koleo API.
* **Pagination:** Easily browse earlier or later train connections.
* **Smart Seat Finder:** The app checks seat availability for every single part of the route, not just the full trip.

## Tech Stack

* **Backend:** Kotlin, Ktor (Server & Client), Koin (Dependency Injection), and `kotlinx.serialization` for JSON parsing.
* **Frontend:** HTML5, CSS3, Vanilla JavaScript, and Flatpickr for date selection.
* **External APIs:** Integrates with the Koleo API (v2) to get up-to-date station, connection, and real-time seat data.

## How the Seat Algorithm Works

1. If a seat is not available for the whole trip, the app divides the route into smaller segments (between every single stop).
2. It asks the API for available seats on each small segment.
3. The scoring engine tracks these segments for each seat. It calculates the total continuous time a passenger can sit in a specific seat.
4. It sorts the results and returns the best options, showing exactly which stops the seat is valid for.

## Getting Started

### Prerequisites
* JDK 21+
* Gradle

### Running Locally
1. Clone the repository.
2. Run the Ktor backend application using your IDE or Gradle (`./gradlew run`).
3. Open your web browser and go to `http://localhost:8080`.