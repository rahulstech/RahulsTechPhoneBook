package rahulstech.android.phonebook.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NameTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void splitDisplayName() {
        Name name = new Name();
        name.setDisplayName("Mr. Rohan Kumar Rajan Das jr.");
        name.splitDisplayName();

        assertEquals("prefix not matched","Mr.",name.getPrefix());
        assertEquals("given name not matched","Rohan",name.getGivenName());
        assertEquals("middle name not matched","Kumar Rajan",name.getMiddleName());
        assertEquals("family name not matched","Das",name.getFamilyName());
        assertEquals("suffix not matched","jr.",name.getSuffix());
    }

    @Test
    public void splitDisplayName_noPrefixSuffix() {
        Name name = new Name();
        name.setDisplayName("Rohan Kumar Rajan Das");
        name.splitDisplayName();

        assertNull("prefix must be null",name.getPrefix());
        assertEquals("given name not matched","Rohan",name.getGivenName());
        assertEquals("middle name not matched","Kumar Rajan",name.getMiddleName());
        assertEquals("family name not matched","Das",name.getFamilyName());
        assertNull("suffix must be null",name.getSuffix());
    }

    @Test
    public void splitDisplayName_onlyGivenName() {
        Name name = new Name();
        name.setDisplayName("Rohan");
        name.splitDisplayName();

        assertNull("prefix must be null",name.getPrefix());
        assertEquals("given name not matched","Rohan",name.getGivenName());
        assertNull("middle name must be null",name.getMiddleName());
        assertNull("family name must be null",name.getFamilyName());
        assertNull("suffix must be null",name.getSuffix());
    }

    @Test
    public void splitDisplayName_onlyGivenNameAndFamilyName() {
        Name name = new Name();
        name.setDisplayName("Rohan Das");
        name.splitDisplayName();

        assertNull("prefix must be null",name.getPrefix());
        assertEquals("given name not matched","Rohan",name.getGivenName());
        assertNull("middle name must be null",name.getMiddleName());
        assertEquals("family name not matched","Das",name.getFamilyName());
        assertNull("suffix must be null",name.getSuffix());
    }

    @Test
    public void splitDisplayName_emptyDisplayName() {
        Name name = new Name();
        name.splitDisplayName();

        assertNull("prefix must be null",name.getPrefix());
        assertNull("given name must be null",name.getGivenName());
        assertNull("middle name must be null",name.getMiddleName());
        assertNull("family name must be null",name.getFamilyName());
        assertNull("suffix must be null",name.getSuffix());
    }
}