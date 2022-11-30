package rahulstech.android.phonebook.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class Website {

    private long id;

    @NonNull
    private String url;

    public Website(long id, String url) {
        this.id = id;
        this.url = url;
    }

    public Website() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Website)) return false;
        Website website = (Website) o;
        return id == website.id
                && Check.isEquals(url, website.url);
    }
}
