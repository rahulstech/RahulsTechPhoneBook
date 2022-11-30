package rahulstech.android.phonebook.model;

import rahulstech.android.phonebook.util.Check;

import static rahulstech.android.phonebook.util.Helpers.anyNonEmpty;

public class Organization {

    private long id;

    private String company;

    private String title;

    private String department;

    public Organization(long id, String company, String title, String department) {
        this.id = id;
        this.company = company;
        this.title = title;
        this.department = department;
    }

    public Organization(){}

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

    public String buildDisplayText() {
        StringBuilder display = new StringBuilder();
        if (!Check.isEmptyString(company)) display.append(company);
        if (!Check.isEmptyString(title)) display.append('\n').append(title);
        if (!Check.isEmptyString(department)) display.append('\n').append(department);
        return display.toString();
    }

    public boolean hasValues() {
        return anyNonEmpty(company,title,department);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization)) return false;
        Organization that = (Organization) o;
        return id == that.id
                && Check.isEquals(company, that.company)
                && Check.isEquals(title, that.title)
                && Check.isEquals(department, that.department);
    }
}
