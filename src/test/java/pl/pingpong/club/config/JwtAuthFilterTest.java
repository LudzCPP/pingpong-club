package pl.pingpong.club.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks JwtAuthFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Brak tokenu ──────────────────────────────────────────────────────────

    @Test
    void noAuthHeader_passesThroughWithoutSettingContext() throws Exception {
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull(); // chain was called
    }

    @Test
    void notBearerPrefix_passesThroughWithoutSettingContext() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    // ── Nieważny / wygasły token ─────────────────────────────────────────────

    @Test
    void unparsableToken_returns401AndStopsChain() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer definitely-not-a-jwt");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        given(jwtService.extractEmail("definitely-not-a-jwt"))
                .willThrow(new RuntimeException("Malformed JWT"));

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull(); // chain was NOT called
    }

    @Test
    void expiredToken_returns401AndStopsChain() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer expired-token");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        UserDetails userDetails = mock(UserDetails.class);
        given(jwtService.extractEmail("expired-token")).willReturn("coach@test.pl");
        given(userDetailsService.loadUserByUsername("coach@test.pl")).willReturn(userDetails);
        given(jwtService.isTokenValid("expired-token", userDetails)).willReturn(false);

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    // ── Ważny token ──────────────────────────────────────────────────────────

    @Test
    void validToken_setsSecurityContextAndContinuesChain() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        UserDetails userDetails = mock(UserDetails.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_COACH")))
                .when(userDetails).getAuthorities();
        given(jwtService.extractEmail("valid-token")).willReturn("coach@test.pl");
        given(userDetailsService.loadUserByUsername("coach@test.pl")).willReturn(userDetails);
        given(jwtService.isTokenValid("valid-token", userDetails)).willReturn(true);

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        assertThat(chain.getRequest()).isNotNull();
    }
}
