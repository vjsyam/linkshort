package com.linkshort.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Base62Test {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    @DisplayName("encodes only with Base62 characters")
    void usesOnlyBase62Characters() {
        String code = Base62.encode(987_654_321L);
        for (char c : code.toCharArray()) {
            assertTrue(ALPHABET.indexOf(c) >= 0, "unexpected character: " + c);
        }
    }

    @Test
    @DisplayName("decode reverses encode")
    void roundTrip() {
        for (long id : new long[]{1, 61, 62, 3843, 3844, 123_456_789L}) {
            assertEquals(id, Base62.decode(Base62.encode(id)));
        }
    }

    @Test
    @DisplayName("different ids never share a code")
    void codesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (long id = 1; id <= 10_000; id++) {
            assertTrue(seen.add(Base62.encode(id)), "duplicate code for id " + id);
        }
    }

    @Test
    @DisplayName("larger ids give equal or longer codes")
    void codeLengthGrowsWithId() {
        assertTrue(Base62.encode(62).length() >= Base62.encode(61).length());
    }

    @Test
    @DisplayName("negative and zero ids are rejected")
    void rejectsNegativeAndZeroId() {
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(0));
    }

    @Test
    @DisplayName("invalid characters in decode are rejected")
    void rejectsInvalidCharactersInDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("abc!"));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode(""));
    }
}
