package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import rahulstech.android.phonebook.util.Check;

public class Organization {

    private String lookupKey;

    private long id;

    private String company;

    private String title;

    private String department;

    private String jobDescription;

    private String officeLocation;

    private int type;

    private CharSequence typeLabel;

    public Organization(String lookupKey, long id, String company, String title, String department, String jobDescription, String officeLocation, int type, CharSequence typeLabel) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.company = company;
        this.title = title;
        this.department = department;
        this.jobDescription = jobDescription;
        this.officeLocation = officeLocation;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Organization(){}

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getOfficeLocation() {
        return officeLocation;
    }

    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Organization.getTypeLabel(res,type,typeLabel);
    }

    public void setTypeLabel(CharSequence typeLabel) {
        this.typeLabel = typeLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization)) return false;
        Organization that = (Organization) o;
        return id == that.id
                && Check.isEquals(lookupKey, that.lookupKey)
                && Check.isEquals(company, that.company)
                && Check.isEquals(title, that.title)
                && Check.isEquals(department, that.department)
                && Check.isEquals(jobDescription, that.jobDescription)
                && Check.isEquals(officeLocation, that.officeLocation)
                && type == that.type
                && Check.isEquals(typeLabel,that.typeLabel);
    }
}
