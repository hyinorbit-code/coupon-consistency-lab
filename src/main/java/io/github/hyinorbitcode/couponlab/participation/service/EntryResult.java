package io.github.hyinorbitcode.couponlab.participation.service;

import io.github.hyinorbitcode.couponlab.drop.PublishedDrop;
import io.github.hyinorbitcode.couponlab.prizecode.AssignedPrize;

public record EntryResult(PublishedDrop drop, AssignedPrize prize, boolean newlyWon) {
}