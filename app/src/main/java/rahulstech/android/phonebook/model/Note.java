package rahulstech.android.phonebook.model;

import rahulstech.android.phonebook.util.Check;

public class Note {

    private long id;

    private String note;

    public Note( long id, String note) {
        this.id = id;
        this.note = note;
    }

    public Note(){}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean hasNote() {
        return !Check.isEmptyString(note);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note1 = (Note) o;
        return id == note1.id
                && Check.isEquals(note, note1.note);
    }
}
