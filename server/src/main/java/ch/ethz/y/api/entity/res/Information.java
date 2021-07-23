package ch.ethz.y.api.entity.res;

import java.time.LocalDate;

public class Information {

    private int apiVersion = 1;
    private LocalDate deprecationDate = null;
    private String deprecationInfo = null;

    public int getApiVersion() {
        return apiVersion;
    }

    public Information setApiVersion(int apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public LocalDate getDeprecationDate() {
        return deprecationDate;
    }

    public Information setDeprecationDate(LocalDate deprecationDate) {
        this.deprecationDate = deprecationDate;
        return this;
    }

    public String getDeprecationInfo() {
        return deprecationInfo;
    }

    public Information setDeprecationInfo(String deprecationInfo) {
        this.deprecationInfo = deprecationInfo;
        return this;
    }
}
