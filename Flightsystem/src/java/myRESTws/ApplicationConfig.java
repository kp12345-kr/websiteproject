/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package myRESTws;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author Krish
 */
@javax.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(myRESTws.BookingResource.class);
        resources.add(myRESTws.CurCodesRS.class);
        resources.add(myRESTws.FlightResource.class);
        resources.add(myRESTws.UserloginResource.class);
        resources.add(myRESTws.adminloginResource.class);
    }
    
}
