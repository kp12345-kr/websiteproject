/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package myRESTws;

import org.json.JSONObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("adminlogin")
public class adminloginResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminLogin(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String username = json.getString("username");
            String password = json.getString("password");

            if ("admin".equals(username) && "password".equals(password)) {
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("message", "Login successful");
                responseJson.put("redirectUrl", "addflight.html");
                return Response.ok(responseJson.toString()).build();
            } else {
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", false);
                responseJson.put("message", "Invalid credentials");
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity(responseJson.toString())
                               .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Invalid JSON data")
                           .build();
        }
    }
}
