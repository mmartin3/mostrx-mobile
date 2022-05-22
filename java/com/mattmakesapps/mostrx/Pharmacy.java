package com.mattmakesapps.mostrx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

public class Pharmacy {

    @SerializedName("Zip")
    @Expose
    public String zip;
    @SerializedName("Distance")
    @Expose
    public String distance;
    @SerializedName("PharmacyName")
    @Expose
    public String pharmacyName;
    @SerializedName("Address1")
    @Expose
    public String address1;
    @SerializedName("Address2")
    @Expose
    public String address2;
    @SerializedName("City")
    @Expose
    public String city;
    @SerializedName("State")
    @Expose
    public String state;
    @SerializedName("Phone")
    @Expose
    public String phone;
    @SerializedName("NCPDP")
    @Expose
    public String ncpdp;
    @SerializedName("long")
    @Expose
    public String longitude;
    @SerializedName("lat")
    @Expose
    public String latitude;
    @SerializedName("NPI")
    @Expose
    public String npi;

    public String getName() {
        String[] split = pharmacyName.split("\\s+");

        if (split.length == 1) {
            return pharmacyName;
        } else {
            int last = split.length - 1;
            split[last] = split[last].replaceAll("[0-9]", "");
        }

        return StringUtils.join(split, " ");
    }
}
