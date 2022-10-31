package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;

import rahulstech.android.phonebook.repository.ModelException;
import rahulstech.android.phonebook.util.Check;

public class PostalAddress {

    private String lookupKey;

    private long id;
    
    private String formattedAddress;
    
    private String street;
    
    private String postBox;
    
    private String neighbourhood;
    
    private String city;
    
    private String region;
    
    private String postalCode;
    
    private String country;
    
    private int type;
    
    private CharSequence typeLabel;

    public PostalAddress(String lookupKey, long id, String formattedAddress, String street, String postBox, String neighbourhood, String city, String region, String postalCode, String country, int type, CharSequence typeLabel) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.formattedAddress = formattedAddress;
        this.street = street;
        this.postBox = postBox;
        this.neighbourhood = neighbourhood;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public long getId() {
        return id;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public String getStreet() {
        return street;
    }

    public String getPostBox() {
        return postBox;
    }

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public int getType() {
        return type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(res,type,typeLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostalAddress)) return false;
        PostalAddress that = (PostalAddress) o;
        return type == that.type
                && id == that.id
                && Check.isEquals(lookupKey, that.lookupKey)
                && Check.isEquals(formattedAddress, that.formattedAddress)
                && Check.isEquals(street, that.street)
                && Check.isEquals(postBox, that.postBox)
                && Check.isEquals(neighbourhood, that.neighbourhood)
                && Check.isEquals(city, that.city)
                && Check.isEquals(region, that.region)
                && Check.isEquals(postalCode, that.postalCode)
                && Check.isEquals(country, that.country)
                && Check.isEquals(typeLabel, that.typeLabel);
    }
}
