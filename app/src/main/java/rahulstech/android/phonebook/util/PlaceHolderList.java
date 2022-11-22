package rahulstech.android.phonebook.util;

import java.util.AbstractList;

public class PlaceHolderList<I> extends AbstractList<I> {

    private final int size;

    public PlaceHolderList(int size) {
        Check.isTrue(size >= 0,"size < 0");
        this.size = size;
    }

    @Override
    public I get(int index) {
        if (index >= 0 && index < size) return null;
        throw new IndexOutOfBoundsException("valid index 0-"+(size-1)+" requested "+index);
    }

    @Override
    public int size() {
        return size;
    }
}
