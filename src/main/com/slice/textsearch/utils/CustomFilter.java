package com.slice.textsearch.utils;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

@WebFilter(urlPatterns = "/*")
public class CustomFilter implements Filter {
	Pattern[] restPatterns = new Pattern[] { Pattern.compile("/searcher.*") };

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			String path = ((HttpServletRequest) request).getServletPath();
			for (Pattern pattern : restPatterns) {
				if (pattern.matcher(path).matches()) {
					String newPath = "/v1/" + path;
					request.getRequestDispatcher(newPath).forward(request,
							response);
					return;
				}
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}