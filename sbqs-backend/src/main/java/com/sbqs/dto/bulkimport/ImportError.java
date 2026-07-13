package com.sbqs.dto.bulkimport;

public final class ImportError {
    private final int row;
    private final String identifier;
    private final String message;

    public ImportError(int row, String identifier, String message) {
        this.row = row;
        this.identifier = identifier;
        this.message = message;
    }

    public int getRow() { return row; }
    public String getIdentifier() { return identifier; }
    public String getMessage() { return message; }
}
