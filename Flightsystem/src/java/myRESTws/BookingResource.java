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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * REST Web Service
 *
 * @author Krish
 */
@Path("booking")
public class BookingResource {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/flightdatabase";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123455";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bookFlight(String requestBody) {
        JSONObject request = new JSONObject(requestBody);
        int flightId = request.getInt("flightId");
        String name = request.getString("name");
        String email = request.getString("email");
        int adults = request.getInt("adults");
        int children = request.getInt("children");
        String passport = request.getString("passport");
        String cardNumber = request.getString("cardNumber");
        String expiryDate = request.getString("expiryDate");
        String cvv = request.getString("cvv");

        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false);

            String insertBookingSQL = "INSERT INTO bookings (flight_id, name, email, adults, children, passport, card_number, expiry_date, cvv) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String updateSeatsSQL = "UPDATE flights4 SET available_seats = available_seats - ? WHERE flight_id = ? AND available_seats >= ?";

            try (PreparedStatement updateSeatsStmt = conn.prepareStatement(updateSeatsSQL);
                 PreparedStatement insertBookingStmt = conn.prepareStatement(insertBookingSQL)) {

                int totalPassengers = adults + children;
                updateSeatsStmt.setInt(1, totalPassengers);
                updateSeatsStmt.setInt(2, flightId);
                updateSeatsStmt.setInt(3, totalPassengers);
                int rowsAffected = updateSeatsStmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new SQLException("Not enough available seats for flight ID: " + flightId);
                }

                insertBookingStmt.setInt(1, flightId);
                insertBookingStmt.setString(2, name);
                insertBookingStmt.setString(3, email);
                insertBookingStmt.setInt(4, adults);
                insertBookingStmt.setInt(5, children);
                insertBookingStmt.setString(6, passport);
                insertBookingStmt.setString(7, cardNumber);
                insertBookingStmt.setString(8, expiryDate);
                insertBookingStmt.setString(9, cvv);
                insertBookingStmt.executeUpdate();

                conn.commit();

                JSONObject response = new JSONObject();
                response.put("message", "Booking successful");
                return Response.ok(response.toString()).build();

            } catch (SQLException e) {
                conn.rollback();
                JSONObject response = new JSONObject();
                response.put("message", "Booking failed: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response.toString()).build();
            }
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("message", "Internal server error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response.toString()).build();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookings() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String selectBookingsSQL = "SELECT * FROM bookings";
            try (PreparedStatement selectBookingsStmt = conn.prepareStatement(selectBookingsSQL);
                 ResultSet rs = selectBookingsStmt.executeQuery()) {

                JSONArray bookingsArray = new JSONArray();
                while (rs.next()) {
                    JSONObject booking = new JSONObject();
                    booking.put("id", rs.getInt("booking_id")); // Correct column name
                    booking.put("flight_id", rs.getInt("flight_id"));
                    booking.put("name", rs.getString("name"));
                    booking.put("email", rs.getString("email"));
                    booking.put("adults", rs.getInt("adults"));
                    booking.put("children", rs.getInt("children"));
                    booking.put("passport", rs.getString("passport"));
                    booking.put("card_number", rs.getString("card_number"));
                    booking.put("expiry_date", rs.getString("expiry_date"));
                    booking.put("cvv", rs.getString("cvv"));
                    bookingsArray.put(booking);
                }

                return Response.ok(bookingsArray.toString()).build();
            }
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("message", "Internal server error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response.toString()).build();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    @GET
    @Path("discount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response applyDiscount(@QueryParam("flightId") int flightId, @QueryParam("discount") int discount) {
        Connection conn = null;
        JSONArray flightsArray = new JSONArray();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String selectFlightSQL = "SELECT * FROM flights4 WHERE flight_id = ?";
            try (PreparedStatement selectFlightStmt = conn.prepareStatement(selectFlightSQL)) {
                selectFlightStmt.setInt(1, flightId);
                ResultSet rs = selectFlightStmt.executeQuery();

                if (rs.next()) {
                    double originalPrice = rs.getDouble("ticket_price");
                    double discountedPrice = originalPrice * (1 - discount / 100.0);

                    JSONObject flight = new JSONObject();
                    flight.put("flight_id", rs.getInt("flight_id"));
                    flight.put("origin_city", rs.getString("origin_city"));
                    flight.put("destination_city", rs.getString("destination_city"));
                    flight.put("airline", rs.getString("airline"));
                    flight.put("available_seats", rs.getInt("available_seats"));
                    flight.put("connections", rs.getInt("connections"));
                    flight.put("original_price", originalPrice);
                    flight.put("discounted_price", discountedPrice);

                    flightsArray.put(flight);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("message", "Flight not found");
                    return Response.status(Response.Status.NOT_FOUND).entity(response.toString()).build();
                }
            }
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("message", "Internal server error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response.toString()).build();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return Response.ok(flightsArray.toString()).build();
    }
}
