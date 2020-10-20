package jhi.gridscore.server.resource;

import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import org.jooq.DSLContext;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.sql.*;
import java.util.UUID;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

public class ConfigServerResource extends ServerResource
{
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
				String uuid = UUID.randomUUID().toString();

				ConfigurationsRecord record = context.newRecord(CONFIGURATIONS);
				record.setUuid(uuid);
				record.setConfiguration(conf);
				record.store();

				return uuid;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
			}
		}
	}
}
