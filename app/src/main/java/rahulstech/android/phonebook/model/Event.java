package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class Event {

    private long id;

    private String startDate;

    private int type;

    private CharSequence typeLabel;

    public Event(long id, String startDate, int type, CharSequence typeLabel) {
        this.id = id;
        this.startDate = startDate;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Event(){}

    public long getId() {
        return id;
    }

    public String getStartDate() {
        return startDate;
    }

    public int getType() {
        return type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Event.getTypeLabel(res,type,typeLabel);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTypeLabel(CharSequence typeLabel) {
        this.typeLabel = typeLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event event = (Event) o;
        return id == event.id && type == event.type
                && Check.isEquals(startDate, event.startDate)
                && Check.isEquals(typeLabel, event.typeLabel);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", startDate='" + startDate + '\'' +
                ", type=" + type +
                ", typeLabel=" + typeLabel +
                '}';
    }
}
