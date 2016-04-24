package com.slice.textsearch.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NullPointerExceptionMapper implements
		ExceptionMapper<NullPointerException> {

	@Override
	public Response toResponse(NullPointerException exception) {
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity(exception.getClass().toString() + " "
						+ exception.getMessage()).build();
	}

}
