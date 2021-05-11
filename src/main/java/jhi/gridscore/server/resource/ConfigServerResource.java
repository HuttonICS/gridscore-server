package jhi.gridscore.server.resource;

import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

public class ConfigServerResource extends ServerResource
{
	private static final SecureRandom   RANDOM  = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	@Post
	public String postConfiguration(Configuration conf)
	{
		if (conf == null)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
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
			catch (SQLException e)
			{
				e.printStackTrace();
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
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
