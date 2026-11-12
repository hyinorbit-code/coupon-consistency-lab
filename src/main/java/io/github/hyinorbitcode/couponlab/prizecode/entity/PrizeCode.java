package io.github.hyinorbitcode.couponlab.prizecode.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "prize_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrizeCode {

    public static final int MAX_CODE_LENGTH = 100;
    public static final int HINT_LENGTH = 4;
    public static final int HASH_BYTES = 32;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long dropEventId;
    private byte[] codeCiphertext;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = HASH_BYTES)
    private byte[] codeHash;
    private String codeHint;
    private Long assignedUserId;
    private Instant assignedAt;
    private Instant createdAt;

    private PrizeCode(Long dropEventId, byte[] codeCiphertext, byte[] codeHash,
                      String codeHint, Instant createdAt) {
        this.dropEventId = dropEventId;
        this.codeCiphertext = codeCiphertext;
        this.codeHash = codeHash;
        this.codeHint = codeHint;
        this.createdAt = createdAt;
    }

    public static PrizeCode register(long dropEventId, byte[] codeCiphertext, byte[] codeHash,
                                     String codeHint, Instant now) {
        Objects.requireNonNull(codeCiphertext, "codeCiphertext");
        Objects.requireNonNull(codeHash, "codeHash");
        Objects.requireNonNull(codeHint, "codeHint");
        Objects.requireNonNull(now, "now");
        if (codeHash.length != HASH_BYTES) {
            throw new IllegalArgumentException("codeHash must be " + HASH_BYTES + " bytes");
        }
        return new PrizeCode(dropEventId, codeCiphertext, codeHash, codeHint, now);
    }

    public static String hintOf(String code) {
        return code.length() <= HINT_LENGTH ? code : code.substring(code.length() - HINT_LENGTH);
    }

    public boolean isAssigned() {
        return assignedUserId != null;
    }

    public void assignTo(long userId, Instant now) {
        if (isAssigned()) {
            throw new IllegalStateException("already assigned: " + id);
        }
        this.assignedUserId = userId;
        this.assignedAt = Objects.requireNonNull(now, "now");
    }
}