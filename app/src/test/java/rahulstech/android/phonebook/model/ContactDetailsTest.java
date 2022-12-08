package rahulstech.android.phonebook.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rahulstech.android.phonebook.util.ContactSorting;

import static org.junit.Assert.*;

public class ContactDetailsTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void buildSortKey() {
        Name name = new Name();
        name.setPrefix("Mr");
        name.setGivenName("Rohan");
        name.setMiddleName("Kumar");
        name.setFamilyName("Das");
        name.setSuffix("Jr");

        String sortKeyByFirstName = ContactDetails.buildSortKey(name,ContactSorting.FIRSTNAME_FIRST);
        String sortKeyByLastName = ContactDetails.buildSortKey(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("sort key by first name does not match","Rohan",sortKeyByFirstName);
        assertEquals("sort key by last name does not match","Das",sortKeyByLastName);
    }

    @Test
    public void buildSortKey_absentFirstAndLastName() {
        Name name = new Name();
        name.setPrefix("Mr");
        name.setMiddleName("Kumar");
        name.setSuffix("Jr");

        String sortKeyByFirstName = ContactDetails.buildSortKey(name,ContactSorting.FIRSTNAME_FIRST);
        String sortKeyByLastName = ContactDetails.buildSortKey(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("sort key by first name does not match","Kumar",sortKeyByFirstName);
        assertEquals("sort key by last name does not match","Kumar",sortKeyByLastName);
    }

    @Test
    public void buildSortKey_onlyPrefixSuffix() {
        Name name = new Name();
        name.setPrefix("Mr");
        name.setSuffix("Jr");

        String sortKeyByFirstName = ContactDetails.buildSortKey(name,ContactSorting.FIRSTNAME_FIRST);
        String sortKeyByLastName = ContactDetails.buildSortKey(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("sort key by first name does not match","Jr",sortKeyByFirstName);
        assertEquals("sort key by last name does not match","Jr",sortKeyByLastName);
    }

    @Test
    public void buildSortKey_noName() {
        String sortKeyByFirstName = ContactDetails.buildSortKey(null,ContactSorting.FIRSTNAME_FIRST);
        String sortKeyByLastName = ContactDetails.buildSortKey(null,ContactSorting.LASTNAME_FIRST);

        assertNull("sort key by first name does not match",sortKeyByFirstName);
        assertNull("sort key by last name does not match",sortKeyByLastName);
    }

    @Test
    public void buildSortKey_nonAlpha() {
        Name name = new Name();
        name.setGivenName("12345");
        name.setFamilyName("4875");

        String sortKeyByFirstName = ContactDetails.buildSortKey(null,ContactSorting.FIRSTNAME_FIRST);
        String sortKeyByLastName = ContactDetails.buildSortKey(null,ContactSorting.LASTNAME_FIRST);

        assertNull("sort key by first name does not match",sortKeyByFirstName);
        assertNull("sort key by last name does not match",sortKeyByLastName);
    }

    @Test
    public void buildDisplayLabel() {
        Name name = new Name();
        name.setPrefix("Mr");
        name.setGivenName("Rohan");
        name.setMiddleName("Kumar");
        name.setFamilyName("Das");
        name.setSuffix("Jr");

        String displayLabelByFirstName = ContactDetails.buildDisplayLabel(name,ContactSorting.FIRSTNAME_FIRST);
        String displayLabelByLastName = ContactDetails.buildDisplayLabel(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("display label by first name does not match","M",displayLabelByFirstName);
        assertEquals("display label by last name does not match","M",displayLabelByLastName);
    }

    @Test
    public void buildDisplayLabel_onlyFirstName() {
        Name name = new Name();
        name.setGivenName("Rohan");

        String displayLabelByFirstName = ContactDetails.buildDisplayLabel(name,ContactSorting.FIRSTNAME_FIRST);
        String displayLabelByLastName = ContactDetails.buildDisplayLabel(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("display label by first name does not match","R",displayLabelByFirstName);
        assertEquals("display label by last name does not match","R",displayLabelByLastName);
    }

    @Test
    public void buildDisplayLabel_onlyFirstNameLastName() {
        Name name = new Name();
        name.setGivenName("Rohan");
        name.setFamilyName("Das");

        String displayLabelByFirstName = ContactDetails.buildDisplayLabel(name,ContactSorting.FIRSTNAME_FIRST);
        String displayLabelByLastName = ContactDetails.buildDisplayLabel(name,ContactSorting.LASTNAME_FIRST);

        assertEquals("display label by first name does not match","R",displayLabelByFirstName);
        assertEquals("display label by last name does not match","D",displayLabelByLastName);
    }

    @Test
    public void buildDisplayLabel_noName() {
        String displayLabelByFirstName = ContactDetails.buildDisplayLabel(null,ContactSorting.FIRSTNAME_FIRST);
        String displayLabelByLastName = ContactDetails.buildDisplayLabel(null,ContactSorting.LASTNAME_FIRST);

        assertNull("display label by first name does not match",displayLabelByFirstName);
        assertNull("display label by last name does not match",displayLabelByLastName);
    }

    @Test
    public void buildDisplayLabel_nonAlpha() {
        Name name = new Name();
        name.setGivenName("4587");

        String displayLabelByFirstName = ContactDetails.buildDisplayLabel(name,ContactSorting.FIRSTNAME_FIRST);
        String displayLabelByLastName = ContactDetails.buildDisplayLabel(name,ContactSorting.LASTNAME_FIRST);

        assertNull("display label by first name does not match",displayLabelByFirstName);
        assertNull("display label by last name does not match",displayLabelByLastName);
    }
}