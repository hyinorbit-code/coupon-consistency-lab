package io.github.hyinorbitcode.couponlab.drop;

public interface DropEventApi {
    void checkCodesRegistrable(long dropEventId);

    PublishedDrop getPublishedDrop(long dropEventId);

    void checkEntryOpen(long dropEventId);
}
