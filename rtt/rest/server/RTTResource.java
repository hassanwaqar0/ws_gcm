package it.polito.ws_vs_gcm.rtt.rest.server;

@Path("RTTEvaluation/{SN}/{PAYLOAD}")
public class RTTResource {

    @Context
    private UriInfo context;

    public RTTResource() {
    }

    @GET
    @Produces("application/json")
    public String getJson(@PathParam("SN") String i, @PathParam("PAYLOAD") String payload) throws JSONException {
        long time1 = System.currentTimeMillis();
        JSONObject js = new JSONObject();
        js.put("SN",i);
        js.put("PAYLOAD",payload);
        return js.toString();
    }

    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }
}
