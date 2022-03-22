package jhi.gridscore.server.resource;

import jakarta.ws.rs.core.*;

import jakarta.servlet.http.*;

public class ContextResource
{
	@Context
	protected SecurityContext    securityContext;
	@Context
	protected HttpServletRequest req;
	@Context
	protected HttpServletResponse resp;
}
