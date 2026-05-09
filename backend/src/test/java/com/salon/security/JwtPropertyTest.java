package com.salon.security;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

/**
 * Property-based tests for JwtUtil.
 *
 * Validates: Requirements 5.1
 */
public class JwtPropertyTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "test_jwt_secret_key_must_be_at_least_256_bits_long_for_testing");

        Field expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, 86400000L);
    }

    /**
     * Property 5: JWT Claims Completeness
     *
     * For any successful authentication, the returned JWT token should decode to contain
     * at minimum: role, userId, and email fields.
     *
     * Validates: Requirements 5.1
     */
    @Property
    void jwtAlwaysContainsRoleUserIdAndEmail(
            @ForAll @StringLength(min = 3, max = 50) @AlphaChars String emailPrefix,
            @ForAll @StringLength(min = 1, max = 20) @AlphaChars String role,
            @ForAll @LongRange(min = 1, max = 10000) long userId) throws Exception {

        // Re-initialize jwtUtil for each property run since @BeforeEach doesn't run per try
        JwtUtil util = new JwtUtil();
        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(util, "test_jwt_secret_key_must_be_at_least_256_bits_long_for_testing");
        Field expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(util, 86400000L);

        String email = emailPrefix + "@test.com";
        String token = util.generateToken(email, role, userId, null);

        assert token != null && !token.isEmpty();
        assert util.extractEmail(token).equals(email);
        assert util.extractRole(token).equals(role);
        assert util.extractUserId(token).equals(userId);
        assert util.isTokenValid(token);
    }
}
