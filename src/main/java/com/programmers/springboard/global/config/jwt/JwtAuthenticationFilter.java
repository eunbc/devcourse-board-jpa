package com.programmers.springboard.global.config.jwt;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.logging.log4j.util.Strings.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends GenericFilterBean {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String headerKey;
	private final Jwt jwt;

	public JwtAuthenticationFilter(String headerKey, Jwt jwt) {
		this.headerKey = headerKey;
		this.jwt = jwt;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)res;

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = getToken(request);
			if (token != null) {
				try {
					Jwt.Claims claims = verify(token);
					log.debug("Jwt parse result: {}", claims);

					String username = claims.username;
					List<GrantedAuthority> authorities = getAuthorities(claims);

					if (isNotEmpty(username) && !authorities.isEmpty()) {
						JwtAuthenticationToken authentication =
							new JwtAuthenticationToken(new JwtAuthentication(token, username), null, authorities);
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);
					}
				} catch (Exception e) {
					log.warn("Jwt processing failed: {}", e.getMessage());
				}
			}
		} else {
			log.debug("SecurityContextHolder not populated with security token, as it already contained: '{}'",
				SecurityContextHolder.getContext().getAuthentication());
		}

		chain.doFilter(request, response);
	}

	private String getToken(HttpServletRequest request) {
		String token = request.getHeader(headerKey);
		if (isNotEmpty(token)) {
			log.debug("Jwt authorization api detected: {}", token);
			try {
				return URLDecoder.decode(token, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	private Jwt.Claims verify(String token) {
		return jwt.verify(token);
	}

	private List<GrantedAuthority> getAuthorities(Jwt.Claims claims) {
		String[] roles = claims.roles;
		return roles == null || roles.length == 0 ?
			emptyList() :
			Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(toList());
	}
}
