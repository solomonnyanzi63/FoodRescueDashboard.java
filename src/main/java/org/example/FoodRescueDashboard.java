package org.example;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FoodRescueDashboard extends JFrame {

    private static final String CONNECTION_URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "FoodRescueDB";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> donationsCollection;
    private MongoCollection<Document> donorsCollection;

    // UI Input Fields
    private JComboBox<String> donorComboBox;
    private JTextField foodItemField;
    private JTextField weightField;
    private JComboBox<String> tempRequirementBox;

    // UI Table Elements for Live View
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JLabel totalWeightLabel;

    public FoodRescueDashboard() {
        // 1. Establish Background Connections
        initDatabase();

        // 2. Setup Frame Layout
        setTitle("Food Rescue Network - Central Logistics Dashboard");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // Create Left Panel: Entry Form
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Log New Donation Package"));

        JPanel formGrid = new JPanel(new GridLayout(4, 2, 10, 15));
        formGrid.add(new JLabel("Active Donor Source:"));
        donorComboBox = new JComboBox<>(fetchDonorNames());
        formGrid.add(donorComboBox);

        formGrid.add(new JLabel("Item Description:"));
        foodItemField = new JTextField();
        formGrid.add(foodItemField);

        formGrid.add(new JLabel("Net Weight (kg):"));
        weightField = new JTextField();
        formGrid.add(weightField);

        formGrid.add(new JLabel("Storage Environment:"));
        tempRequirementBox = new JComboBox<>(new String[]{"Ambient", "Refrigerated", "Frozen"});
        formGrid.add(tempRequirementBox);

        JButton submitButton = new JButton("Publish to MongoDB");
        submitButton.setBackground(new Color(40, 167, 69));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 12));

        leftPanel.add(formGrid, BorderLayout.CENTER);
        leftPanel.add(submitButton, BorderLayout.SOUTH);

        // Create Right Panel: Spatial Search Mock Tool
        JPanel rightPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Geospatial Proximity Routing"));
        JTextArea geoOutputArea = new JTextArea("Click below to scan for the nearest hub using MongoDB 2dsphere indexing simulation...");
        geoOutputArea.setLineWrap(true);
        geoOutputArea.setEditable(false);
        JButton geoSearchButton = new JButton("📍 Match Nearest Recipient Shelter");
        geoSearchButton.setBackground(new Color(0, 123, 255));
        geoSearchButton.setForeground(Color.WHITE);

        rightPanel.add(new JLabel("Simulated Coordinate Search Engine", SwingConstants.CENTER));
        rightPanel.add(new JScrollPane(geoOutputArea));
        rightPanel.add(geoSearchButton);

        // Combine inputs into a Top Dashboard Panel
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(leftPanel);
        topPanel.add(rightPanel);
        add(topPanel, BorderLayout.NORTH);

        // Create Bottom Panel: Live JTable Data Grid
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Live Active Donations (Stored in MongoDB Database)"));

        String[] columnNames = {"Document ID", "Donor Source", "Description", "Weight (kg)", "Environment", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);
        dataTable.setFillsViewportHeight(true);

        JPanel metricsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalWeightLabel = new JLabel("Aggregate Tracked Weight: 0.00 kg  | ");
        JButton refreshButton = new JButton("🔄 Manual Force Refresh");
        metricsPanel.add(totalWeightLabel);
        metricsPanel.add(refreshButton);

        bottomPanel.add(new JScrollPane(dataTable), BorderLayout.CENTER);
        bottomPanel.add(metricsPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.CENTER);

        // 3. Controller Actions & Action Listeners
        submitButton.addActionListener(e -> handleDonationSubmission());
        refreshButton.addActionListener(e -> refreshLiveTableData());
        geoSearchButton.addActionListener(e -> simulateGeospatialQuery(geoOutputArea));

        // Initial table population load
        refreshLiveTableData();
    }

    private void initDatabase() {
        try {
            mongoClient = MongoClients.create(CONNECTION_URI);
            database = mongoClient.getDatabase(DATABASE_NAME);
            donationsCollection = database.getCollection("food_donations");
            donorsCollection = database.getCollection("donors");

            // Generate boilerplate seed documents if collections are entirely blank
            if (donorsCollection.countDocuments() == 0) {
                donorsCollection.insertOne(new Document("name", "Green Grocers Market").append("location", new Document("type", "Point").append("coordinates", List.of(32.5825, 0.3476))));
                donorsCollection.insertOne(new Document("name", "The Daily Bread Bakery").append("location", new Document("type", "Point").append("coordinates", List.of(32.5711, 0.3521))));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Connection error to background MongoDB context: " + ex.getMessage(),
                    "Database Disconnected", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] fetchDonorNames() {
        List<String> donors = new ArrayList<>();
        try {
            for (Document doc : donorsCollection.find()) {
                donors.add(doc.getString("name"));
            }
        } catch (Exception ignored) {}
        if (donors.isEmpty()) donors.add("Default Local Hub Store");
        return donors.toArray(new String[0]);
    }

    private void handleDonationSubmission() {
        String selectedDonor = (String) donorComboBox.getSelectedItem();
        String foodItem = foodItemField.getText().trim();
        String weightText = weightField.getText().trim();
        String tempReq = (String) tempRequirementBox.getSelectedItem();

        if (foodItem.isEmpty() || weightText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All input criteria parameters must be populated.", "Form Field Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double weight = Double.parseDouble(weightText);

            // Constructing standard BSON document map hierarchy
            Document donationDocument = new Document()
                    .append("donor_name", selectedDonor)
                    .append("item_description", foodItem)
                    .append("weight_kg", weight)
                    .append("logistics_metadata", new Document()
                            .append("temp_requirement", tempReq)
                            .append("status", "Unassigned")
                            .append("timestamp", Instant.now().toString()));

            donationsCollection.insertOne(donationDocument);

            // Clear input layouts for next loop write sequence
            foodItemField.setText("");
            weightField.setText("");

            // Trigger automatic live reactive table window updates
            refreshLiveTableData();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Weight must explicitly resolve to a floating-point integer format.", "Data Typo Check", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void refreshLiveTableData() {
        tableModel.setRowCount(0); // Wipe stale data records clear
        double runningWeightAccumulator = 0.0;

        try {
            for (Document doc : donationsCollection.find()) {
                ObjectId id = doc.getObjectId("_id");
                String donor = doc.getString("donor_name");
                String desc = doc.getString("item_description");
                Double weight = doc.getDouble("weight_kg");

                Document metadata = (Document) doc.get("logistics_metadata");
                String environment = (metadata != null) ? metadata.getString("temp_requirement") : "Ambient";
                String status = (metadata != null) ? metadata.getString("status") : "Unassigned";

                runningWeightAccumulator += (weight != null) ? weight : 0.0;

                tableModel.addRow(new Object[]{
                        id != null ? id.toHexString() : "N/A",
                        donor,
                        desc,
                        weight != null ? String.format("%.2f", weight) : "0.00",
                        environment,
                        status
                });
            }
            totalWeightLabel.setText(String.format("Aggregate Tracked Weight: %.2f kg  | ", runningWeightAccumulator));
        } catch (Exception ex) {
            System.err.println("Fatal stream parsing loop failure: " + ex.getMessage());
        }
    }

    /**
     * Simulates a Geospatial pipeline utilizing a lookup strategy inside a background log engine
     */
    private void simulateGeospatialQuery(JTextArea loggingConsole) {
        loggingConsole.setText("Executing Native MongoDB Query Optimization...\n");
        loggingConsole.append("Pipeline Target: Filters.near(\"location\", GeoJSON_Point, maxDistance_meters)\n\n");

        try {
            Document sampleShelterMatch = database.getCollection("recipients").find().first();
            if (sampleShelterMatch != null) {
                loggingConsole.append("🎯 Optimal Match Found: " + sampleShelterMatch.getString("name") + "\n");
                loggingConsole.append("Operating Window: " + sampleShelterMatch.getString("hours") + "\n");
            } else {
                loggingConsole.append("🎯 Optimal Match Found: Hope Children's Shelter Hub\n");
                loggingConsole.append("Distance Vector: 1.2 Kilometers away.\n");
                loggingConsole.append("Routing Status: High Capacity Available (Ambient Storage Open).");
            }
        } catch (Exception ex) {
            loggingConsole.append("Query execution halted: " + ex.getMessage());
        }
    }

    // --- THIS IS THE MISSING MAIN METHOD TO START YOUR APP ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FoodRescueDashboard().setVisible(true);
        });
    }
}
