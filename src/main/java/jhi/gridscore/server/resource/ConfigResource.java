package jhi.gridscore.server.resource;

import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

@Path("config")
public class ConfigResource extends ContextResource
{
	private static final SecureRandom   RANDOM  = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String postConfiguration(Configuration conf)
		throws IOException, SQLException
	{
		if (conf == null)
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return null;
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				// Has an id been provided?
				if (!StringUtils.isEmpty(conf.getUuid()))
				{
					// Get the old config
					ConfigurationsRecord dbConf = context.selectFrom(CONFIGURATIONS).where(CONFIGURATIONS.UUID.eq(conf.getUuid())).fetchAny();

					if (dbConf != null)
					{
						// Update it
						dbConf.setConfiguration(conf);
						dbConf.setCreatedOn(new Timestamp(System.currentTimeMillis()));
						dbConf.store();

						// Return the same id
						return conf.getUuid();
					}
					else
					{
						// Doesn't exist, create it
						return addNewConfig(context, conf);
					}
				}
				else
				{
					// No id provided, create it
					return addNewConfig(context, conf);
				}
			}
		}
	}

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Configuration getConfiguration(@PathParam("id") String id)
		throws SQLException, IOException
	{
		if (StringUtils.isEmpty(id))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return null;
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				ConfigurationsRecord record = context.selectFrom(CONFIGURATIONS)
													 .where(CONFIGURATIONS.UUID.eq(id))
													 .fetchAny();

				if (record == null)
				{
					resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
					return null;
				}
				else
				{
					Configuration result = record.getConfiguration();
					result.setUuid(id);

					return result;
				}
			}
		}
	}

	private String addNewConfig(DSLContext context, Configuration conf)
	{
		byte[] buffer = new byte[20];
		RANDOM.nextBytes(buffer);
		String id = ENCODER.encodeToString(buffer);

		ConfigurationsRecord record = context.newRecord(CONFIGURATIONS);
		record.setUuid(id);
		record.setConfiguration(conf);
		record.store();

		return id;
	}
}
