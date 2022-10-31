package rahulstech.android.phonebook.model;

import rahulstech.android.phonebook.util.Check;

public class Note {

    private String lookupKey;

    private long id;

    private String note;

    public Note(String lookupKey, long id, String note) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.note = note;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public long getId() {
        return id;
    }

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note1 = (Note) o;
        return id == note1.id
                && Check.isEquals(lookupKey, note1.lookupKey)
                && Check.isEquals(note, note1.note);
    }
}
