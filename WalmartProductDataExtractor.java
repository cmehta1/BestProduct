
import javax.swing.*;
import java.awt.*;
        import java.io.BufferedReader;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.URL;
        import org.json.JSONObject;
        import org.json.JSONArray;

/**
 * SuggestedProductExtractor class provides a graphical interface to fetch and display Walmart product suggestions.
 * The class communicates with SerpAPI to fetch product data and with the Sentiment API to analyze product reviews.
 */
public class WalmartProductDataExtractor
{
    /**
     * The main entry point for the application. Creates the UI and handles user interactions.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Walmart Product Search");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JLabel queryLabel = new JLabel("Enter Search Query:");
            JTextField queryField = new JTextField();
            JButton submitButton = new JButton("Search");

            inputPanel.add(queryLabel, BorderLayout.WEST);
            inputPanel.add(queryField, BorderLayout.CENTER);
            inputPanel.add(submitButton, BorderLayout.EAST);

            JPanel outputPanel = new JPanel();
            outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));

            JScrollPane scrollPane = new JScrollPane(outputPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            submitButton.addActionListener(e -> {
                String searchQuery = queryField.getText().trim();
                if (searchQuery.isEmpty()) {
                    outputPanel.removeAll();
                    outputPanel.add(new JLabel("Please enter a search query."));
                    outputPanel.revalidate();
                    outputPanel.repaint();
                    return;
                }

                String formattedQuery = searchQuery.replace(" ", "+");
                String serpApiEndpoint = "https://serpapi.com/search";
                String serpApiKey = "14160d0c23e488116db65285e3c14397b55d5759b6443b65e7937540bfb42caa";

                try {
                    outputPanel.removeAll();

                    // Add a title above the product list
                    JLabel productListTitle = new JLabel("<html><h2>========OUR PRODUCT RECOMMENDATIONS========:</h2></html>");
                    productListTitle.setHorizontalAlignment(SwingConstants.LEFT);
                    outputPanel.add(productListTitle);

                    String serpApiResponse = callSerpApi(serpApiEndpoint, serpApiKey, formattedQuery);
                    System.out.println(serpApiResponse);

                    JSONObject jsonObject = new JSONObject(serpApiResponse);

                    if (jsonObject.has("organic_results")) {
                        JSONArray products = jsonObject.getJSONArray("organic_results");
                        for (int i = 0; i < Math.min(products.length(), 10); i++) {
                            JSONObject product = products.getJSONObject(i);

                            JPanel productPanel = new JPanel();
                            productPanel.setLayout(new BorderLayout(10, 10));

                            StringBuilder productDetails = new StringBuilder();
                            if (product.has("title")) {
                                productDetails.append("<html><b>").append(product.getString("title")).append("</b><br>");
                            }
                            if (product.has("primary_offer") && product.getJSONObject("primary_offer").has("offer_price")) {
                                productDetails.append("Price: $")
                                        .append(product.getJSONObject("primary_offer").getDouble("offer_price"))
                                        .append("<br>");
                            }
                            if (product.has("rating")) {
                                productDetails.append("Rating: ").append(product.getDouble("rating")).append(" stars<br>");
                            }
                            if (product.has("reviews")) {
                                productDetails.append("Reviews: ").append(product.getInt("reviews")).append("<br>");
                            }
                            productDetails.append("</html>");

                            JLabel detailsLabel = new JLabel(productDetails.toString());
                            productPanel.add(detailsLabel, BorderLayout.CENTER);

                            outputPanel.add(productPanel);
                        }
                    }

                    outputPanel.revalidate();
                    outputPanel.repaint();
                } catch (Exception ex) {
                    outputPanel.removeAll();
                    outputPanel.add(new JLabel("Error: " + ex.getMessage()));
                    outputPanel.revalidate();
                    outputPanel.repaint();
                }
            });

            frame.setLayout(new BorderLayout(10, 10));
            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }

    /**
     * Calls the SerpAPI to fetch Walmart product suggestions based on a query.
     *
     * @param endpoint The API endpoint URL.
     * @param apiKey The API key for authentication.
     * @param query The search query for products.
     * @return The JSON response from SerpAPI.
     * @throws Exception If there is an error during the API call.
     */
    private static String callSerpApi(String endpoint, String apiKey, String query) throws Exception {
        String urlWithParams = endpoint + "?engine=walmart&query=" + query + "&api_key=" + apiKey;

        HttpURLConnection connection = (HttpURLConnection) new URL(urlWithParams).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new Exception("Failed to fetch data from SerpApi. HTTP Code: " + responseCode);
        }
    }
}