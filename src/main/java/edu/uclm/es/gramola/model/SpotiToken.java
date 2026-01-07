package edu.uclm.es.gramola.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotiToken {
    @JsonProperty("access_token")
    public String accessToken;
    
    @JsonProperty("token_type")
    public String tokenType;
    
    @JsonProperty("expires_in")
    public int expiresIn;
    
    @JsonProperty("refresh_token")
    public String refreshToken;
    
    @JsonProperty("scope")
    public String scope;
}