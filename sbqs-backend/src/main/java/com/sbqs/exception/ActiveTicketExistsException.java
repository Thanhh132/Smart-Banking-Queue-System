package com.sbqs.exception;

public class ActiveTicketExistsException extends RuntimeException {
    private final Long ticketId;

    public ActiveTicketExistsException(Long ticketId) {
        super("Bạn đang có phiếu chưa hoàn thành. Hãy hoàn thành hoặc hủy phiếu hiện tại trước.");
        this.ticketId = ticketId;
    }

    public Long getTicketId() {
        return ticketId;
    }
}
