package jhi.gridscore.server;

import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api/")
public class GridScore extends ResourceConfig
{
	public GridScore()
	{
		packages("jhi.gridscore.server");
	}
}
