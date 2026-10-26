package com.bank.account.resource;

import com.bank.account.client.CustomerRestClient;
import com.bank.account.dto.AccountResponseDTO;
import com.bank.account.entity.Account;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.math.BigDecimal;
import java.util.List;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    @RestClient
    CustomerRestClient customerClient;

    // RF2: Obtener cuenta por número
    @GET
    @Path("/{number}")
    public Response getAccountByNumber(@PathParam("number") String number) {
        Account account = Account.find("accountNumber", number).firstResult();
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Cuenta no encontrada\"}").build();
        }
        return Response.ok(AccountResponseDTO.fromEntity(account)).build();
    }

    // RF2: Listar cuentas de un cliente específico
    @GET
    @Path("/customer/{customerId}")
    public List<AccountResponseDTO> getAccountsByCustomer(@PathParam("customerId") Long customerId) {
        return Account.<Account>find("customerId", customerId).stream()
                .map(AccountResponseDTO::fromEntity)
                .toList();
    }

    // Método auxiliar para crear cuentas (valida síncronamente contra customer-service)
    @POST
    @Transactional
    public Response createAccount(@Valid Account account) {
        // 1. Llamada HTTP síncrona al customer-service
        try {
            Response customerResponse = customerClient.getCustomerById(account.customerId);
            if (customerResponse.getStatus() != 200) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"El cliente no existe o está inactivo\"}").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"Servicio de clientes no disponible\"}").build();
        }

        account.persist();
        return Response.status(Response.Status.CREATED).entity(AccountResponseDTO.fromEntity(account)).build();
    }

    // Endpoint interno para debitar saldo (Lo usará el transaction-service)
    @PUT
    @Path("/{number}/debit")
    @Transactional
    public Response debit(@PathParam("number") String number, @QueryParam("amount") BigDecimal amount) {
        Account account = Account.find("accountNumber", number).firstResult();
        if (account == null || account.balance.compareTo(amount) < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Cuenta inválida o saldo insuficiente\"}").build();
        }
        account.balance = account.balance.subtract(amount);
        return Response.ok(AccountResponseDTO.fromEntity(account)).build();
    }

    // Endpoint interno para acreditar saldo (Lo usará el transaction-service)
    @PUT
    @Path("/{number}/credit")
    @Transactional
    public Response credit(@PathParam("number") String number, @QueryParam("amount") BigDecimal amount) {
        Account account = Account.find("accountNumber", number).firstResult();
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Cuenta no encontrada\"}").build();
        }
        account.balance = account.balance.add(amount);
        return Response.ok(AccountResponseDTO.fromEntity(account)).build();
    }
}