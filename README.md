# Food Rescue Dashboard

A Java application connected to MongoDB for managing food donors and tracking food donations.

## Project Structure
* **`src/main/java/org/example/`**: Contains the main Java application source code.
* **`FoodRescueDB.donors`**: Exported MongoDB collection containing sample donor records (including geospatial `Point` coordinates).
* **`FoodRescueDB.food_donatoions`**: Exported MongoDB collection containing food donation records.

## Database Setup (MongoDB)
To run this project locally with the sample data:
1. Ensure MongoDB is running locally on `mongodb://localhost:27017` (`127.0.0.1:27017`).
2. Open **MongoDB Compass** and create a database named **`FoodRescueDB`**.
3. Inside `FoodRescueDB`, create two collections:
   * **`donors`**
   * **`food_donations`**
4. Import **`donors.json`** and **`food_donations.json`** into their matching collections using the **Add Data > Import JSON or CSV file** button in MongoDB Compass.
5. Run the Java application in IntelliJ IDEA.
