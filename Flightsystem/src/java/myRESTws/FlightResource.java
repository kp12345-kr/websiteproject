
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package myRESTws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;


@Path("flights4")
public class FlightResource {

    private static final Map<String, String> CURRENCY_SYMBOLS = new HashMap<>();
    static {
        CURRENCY_SYMBOLS.put("USD", "$");
        CURRENCY_SYMBOLS.put("EUR", "€");
        CURRENCY_SYMBOLS.put("INR", "₹");
        CURRENCY_SYMBOLS.put("AUD", "A$");
        CURRENCY_SYMBOLS.put("GBP", "£");
        CURRENCY_SYMBOLS.put("THB", "฿");
        CURRENCY_SYMBOLS.put("ZAR", "R");
        CURRENCY_SYMBOLS.put("CUP", "₱");
        CURRENCY_SYMBOLS.put("LYD", "LD");
        CURRENCY_SYMBOLS.put("NPR", "₨");
        CURRENCY_SYMBOLS.put("TRY", "₺");
        CURRENCY_SYMBOLS.put("SAR", "﷼");
        CURRENCY_SYMBOLS.put("RON", "lei");
        CURRENCY_SYMBOLS.put("MXN", "₱");
        CURRENCY_SYMBOLS.put("LKR", "₨");
        CURRENCY_SYMBOLS.put("PKR", "₨");
    }

    private double getConversionRate(String currencyCode) {
        double conversionRate = 1.0;
        try {
            String curCodesUrl = "http://localhost:8080/Flightsystem/webresources/curCodes";
            URL url = new URL(curCodesUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONObject jsonResponse = new JSONObject(sb.toString());
            JSONArray currencies = jsonResponse.getJSONArray("CurrConv");
            for (int i = 0; i < currencies.length(); i++) {
                JSONObject currency = currencies.getJSONObject(i);
                if (currency.getString("code").equals(currencyCode)) {
                    conversionRate = currency.getDouble("rate");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return conversionRate;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlights(@QueryParam("currency") String currencyCode) {
        JSONArray flightsArray = new JSONArray();
        String dbUrl = "jdbc:mysql://localhost:3306/flightdatabase";
        String dbUser = "root";
        String dbPassword = "123455";

        double conversionRate = 1.0;
        if (currencyCode != null && !currencyCode.isEmpty()) {
            conversionRate = getConversionRate(currencyCode);
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM flights4")) {

                while (rs.next()) {
                    JSONObject flight = new JSONObject();
                    flight.put("flight_id", rs.getInt("flight_id"));
                    flight.put("origin_city", rs.getString("origin_city"));
                    flight.put("destination_city", rs.getString("destination_city"));
                    flight.put("airline", rs.getString("airline"));
                    flight.put("available_seats", rs.getInt("available_seats"));
                    flight.put("connections", rs.getInt("connections"));
                    
                    double ticketPrice = rs.getDouble("ticket_price");
                    double ticketPriceInSelectedCurrency = ticketPrice * conversionRate;
                    flight.put("ticket_price", ticketPriceInSelectedCurrency);
                    flight.put("currency", currencyCode != null ? currencyCode : "USD");

                    flightsArray.put(flight);
                }
            }
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error.toString()).build();
        }

        return Response.ok(flightsArray.toString()).build();
    }
    
    @GET
    @Path("/convert")
    @Produces(MediaType.TEXT_HTML)
    public Response convertCurrency(@QueryParam("currency") String currencyCode) {
        double conversionRate = getConversionRate(currencyCode);
        String currencyName = CURRENCY_SYMBOLS.getOrDefault(currencyCode, currencyCode); // Use currency code if name not available

        String dbUrl = "jdbc:mysql://localhost:3306/flightdatabase";
        String dbUser = "root";
        String dbPassword = "123455";
        StringBuilder flightsHtml = new StringBuilder("<html><body><h1>Flight Details</h1>");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM flights4")) {

                while (rs.next()) {
                    double ticketPriceInSelectedCurrency = rs.getDouble("ticket_price") * conversionRate;
                    flightsHtml.append("<p><strong>Flight ID:</strong> ")
                            .append(rs.getInt("flight_id"))
                            .append(", <strong>Origin City:</strong> ")
                            .append(rs.getString("origin_city"))
                            .append(", <strong>Destination City:</strong> ")
                            .append(rs.getString("destination_city"))
                            .append(", <strong>Airline:</strong> ")
                            .append(rs.getString("airline"))
                            .append(", <strong>Available Seats:</strong> ")
                            .append(rs.getInt("available_seats"))
                            .append(", <strong>Connections:</strong> ")
                            .append(rs.getInt("connections"))
                            .append(", <strong>Ticket Price:</strong> ")
                            .append(String.format("%.2f", ticketPriceInSelectedCurrency))
                            .append(" ")
                            .append(currencyName)
                            .append("</p>");
                }
            }
        } catch (Exception e) {
            flightsHtml.append("<p>Error: ").append(e.getMessage()).append("</p>");
        }

        flightsHtml.append("</body></html>");
        return Response.ok(flightsHtml.toString()).build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response addFlight(String requestBody) {
        JSONObject request = new JSONObject(requestBody);
        String originCity = request.getString("originCity");
        String destinationCity = request.getString("destinationCity");
        String airline = request.getString("airline");
        int availableSeats = request.getInt("availableSeats");
        int connections = request.getInt("connections");
        double ticketPrice = request.getDouble("ticketPrice");

        String dbUrl = "jdbc:mysql://localhost:3306/flightdatabase";
        String dbUser = "root";
        String dbPassword = "123455";

        String insertFlightSQL = "INSERT INTO flights4 (origin_city, destination_city, airline, available_seats, connections, ticket_price) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(insertFlightSQL)) {

                stmt.setString(1, originCity);
                stmt.setString(2, destinationCity);
                stmt.setString(3, airline);
                stmt.setInt(4, availableSeats);
                stmt.setInt(5, connections);
                stmt.setDouble(6, ticketPrice);

                stmt.executeUpdate();

                JSONObject response = new JSONObject();
                response.put("message", "Flight added successfully");
                return Response.ok(response.toString()).build();
            }
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response.toString()).build();
        }
    }
}
