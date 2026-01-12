package com.alex.voicenotes;

public class ListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NOTE = 1;

    private final int type;
    private final String headerText;
    private final Note note;

    private ListItem(int type, String headerText, Note note) {
        this.type = type;
        this.headerText = headerText;
        this.note = note;
    }

    public static ListItem createHeader(String headerText) {
        return new ListItem(TYPE_HEADER, headerText, null);
    }

    public static ListItem createNote(Note note) {
        return new ListItem(TYPE_NOTE, null, note);
    }

    public int getType() {
        return type;
    }

    public String getHeaderText() {
        return headerText;
    }

    public Note getNote() {
        return note;
    }

    public boolean isHeader() {
        return type == TYPE_HEADER;
    }
}
