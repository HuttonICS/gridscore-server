package jhi.gridscore.server.resource;

import jhi.gridscore.server.PropertyWatcher;
import jhi.gridscore.server.pojo.Settings;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("settings")
public class SettingsResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Settings getSettings()
	{
		Settings result = new Settings();
		result.setPlausibleApiHost(PropertyWatcher.get("plausible.api.host"));
		result.setPlausibleDomain(PropertyWatcher.get("plausible.domain"));
		result.setPlausibleHashMode(PropertyWatcher.getBoolean("plausible.hash.mode"));
		return result;
	}
}
