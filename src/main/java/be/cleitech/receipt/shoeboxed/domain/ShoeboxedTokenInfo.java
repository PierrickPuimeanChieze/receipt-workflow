package be.cleitech.receipt.shoeboxed.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Created by pierrick on 04.02.17.
 */
public class ShoeboxedTokenInfo {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private String scope;

    private Instant lastRefresh;

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @JsonProperty("expires_in")
    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Instant getLastRefresh() {
        return lastRefresh;
    }

    public void setLastRefresh(Instant lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    @Override
    public String toString() {
        return "ShoeboxedTokenInfo{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                ", lastRefresh=" + lastRefresh +
                '}';
    }
}
