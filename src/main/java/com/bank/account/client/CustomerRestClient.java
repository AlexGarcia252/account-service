package com.bank.account.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "customer-api")
@Path("/customers")
public interface CustomerRestClient {

    @GET
    @Path("/{id}")
    Response getCustomerById(@PathParam("id") Long id);
}